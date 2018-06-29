/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.join.QueryBitSetProducer;
import org.apache.lucene.search.join.ScoreMode;
import org.apache.lucene.search.join.ToParentBlockJoinQuery;
import org.hibernate.search.backend.lucene.search.impl.LuceneQueries;
import org.hibernate.search.engine.search.predicate.spi.NestedPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateContributor;


/**
 * @author Guillaume Smet
 */
class NestedPredicateBuilderImpl extends AbstractSearchPredicateBuilder
		implements NestedPredicateBuilder<LuceneSearchPredicateContext, LuceneSearchPredicateCollector> {

	private final String absoluteFieldPath;

	private SearchPredicateContributor<LuceneSearchPredicateContext, ? super LuceneSearchPredicateCollector> nestedPredicateContributor;

	NestedPredicateBuilderImpl(String absoluteFieldPath) {
		this.absoluteFieldPath = absoluteFieldPath;
	}

	@Override
	public void nested(SearchPredicateContributor<LuceneSearchPredicateContext, ? super LuceneSearchPredicateCollector> nestedPredicateContributor) {
		this.nestedPredicateContributor = nestedPredicateContributor;
	}

	@Override
	protected Query buildQuery(LuceneSearchPredicateContext context) {
		BooleanQuery.Builder childQueryBuilder = new BooleanQuery.Builder();
		childQueryBuilder.add( LuceneQueries.childDocumentQuery(), Occur.FILTER );
		childQueryBuilder.add( LuceneQueries.nestedDocumentPathQuery( absoluteFieldPath ), Occur.FILTER );
		childQueryBuilder.add( getQueryFromContributor( new LuceneSearchPredicateContext( absoluteFieldPath ), nestedPredicateContributor ), Occur.MUST );

		Query parentQuery;
		if ( context.getNestedPath() == null ) {
			parentQuery = LuceneQueries.mainDocumentQuery();
		}
		else {
			parentQuery = LuceneQueries.nestedDocumentPathQuery( context.getNestedPath() );
		}

		// TODO at some point we should have a parameter for the score mode
		return new ToParentBlockJoinQuery( childQueryBuilder.build(), new QueryBitSetProducer( parentQuery ), ScoreMode.Avg );
	}
}
