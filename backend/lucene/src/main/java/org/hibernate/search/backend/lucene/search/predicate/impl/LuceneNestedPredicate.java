/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.util.List;

import org.hibernate.search.backend.lucene.lowlevel.query.impl.Queries;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.NestedPredicateBuilder;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.join.BitSetProducer;
import org.apache.lucene.search.join.QueryBitSetProducer;
import org.apache.lucene.search.join.ScoreMode;
import org.apache.lucene.search.join.ToParentBlockJoinQuery;

class LuceneNestedPredicate extends AbstractLuceneSingleFieldPredicate {

	private final LuceneSearchPredicate nestedPredicate;

	private LuceneNestedPredicate(Builder builder) {
		super( builder );
		nestedPredicate = builder.nestedPredicate;
	}

	@Override
	protected Query doToQuery(PredicateRequestContext context) {
		PredicateRequestContext childContext = new PredicateRequestContext( absoluteFieldPath );
		return createNestedQuery( context.getNestedPath(), absoluteFieldPath, nestedPredicate.toQuery( childContext ) );
	}

	public static Query createNestedQuery(String parentNestedDocumentPath, String nestedDocumentPath, Query nestedQuery) {
		if ( nestedDocumentPath.equals( parentNestedDocumentPath ) ) {
			return nestedQuery;
		}

		BooleanQuery.Builder childQueryBuilder = new BooleanQuery.Builder();
		childQueryBuilder.add( Queries.childDocumentQuery(), Occur.FILTER );
		childQueryBuilder.add( Queries.nestedDocumentPathQuery( nestedDocumentPath ), Occur.FILTER );
		childQueryBuilder.add( nestedQuery, Occur.MUST );

		// Note: this filter should include *all* parents, not just the matched ones.
		// Otherwise we will not "see" non-matched parents,
		// and we will consider its matching children as children of the next matching parent.
		BitSetProducer parentFilter = new QueryBitSetProducer( Queries.parentsFilterQuery( parentNestedDocumentPath ) );

		// TODO HSEARCH-3090 at some point we should have a parameter for the score mode
		return new ToParentBlockJoinQuery( childQueryBuilder.build(), parentFilter, ScoreMode.Avg );
	}

	static class Builder extends AbstractBuilder implements NestedPredicateBuilder {
		private LuceneSearchPredicate nestedPredicate;

		Builder(LuceneSearchContext searchContext, String absoluteFieldPath,
				List<String> nestedPathHierarchy) {
			// The given list includes absoluteFieldPath at the end, but here we don't want it to be included.
			super( searchContext, absoluteFieldPath, nestedPathHierarchy.subList( 0, nestedPathHierarchy.size() - 1 ) );
		}

		@Override
		public void nested(SearchPredicate nestedPredicate) {
			LuceneSearchPredicate luceneNestedPredicate = LuceneSearchPredicate.from( searchContext, nestedPredicate );
			luceneNestedPredicate.checkNestableWithin( absoluteFieldPath );
			this.nestedPredicate = luceneNestedPredicate;
		}

		@Override
		public SearchPredicate build() {
			return new LuceneNestedPredicate( this );
		}
	}
}
