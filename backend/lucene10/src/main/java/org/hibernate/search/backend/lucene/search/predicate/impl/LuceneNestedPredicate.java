/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import org.hibernate.search.backend.lucene.lowlevel.query.impl.Queries;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneCompositeNodeSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexCompositeNodeContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.NestedPredicateBuilder;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.join.BitSetProducer;
import org.apache.lucene.search.join.QueryBitSetProducer;
import org.apache.lucene.search.join.ScoreMode;
import org.apache.lucene.search.join.ToParentBlockJoinQuery;

public class LuceneNestedPredicate extends AbstractLuceneSingleFieldPredicate {

	private final LuceneSearchPredicate nestedPredicate;

	private LuceneNestedPredicate(Builder builder) {
		super( builder );
		nestedPredicate = builder.nestedPredicate;
	}

	@Override
	protected Query doToQuery(PredicateRequestContext context) {
		PredicateRequestContext childContext = context.withNestedPath( absoluteFieldPath );
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

	public static class Factory
			extends AbstractLuceneCompositeNodeSearchQueryElementFactory<NestedPredicateBuilder> {
		public static final Factory INSTANCE = new Factory();

		private Factory() {
		}

		@Override
		public NestedPredicateBuilder create(LuceneSearchIndexScope<?> scope, LuceneSearchIndexCompositeNodeContext node) {
			return new Builder( scope, node );
		}
	}

	private static class Builder extends AbstractBuilder implements NestedPredicateBuilder {
		private LuceneSearchPredicate nestedPredicate;

		Builder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexCompositeNodeContext node) {
			super( scope, node.absolutePath(),
					// nestedPathHierarchy includes absoluteFieldPath at the end, but here we don't want it to be included.
					node.nestedPathHierarchy().subList( 0, node.nestedPathHierarchy().size() - 1 ) );
		}

		@Override
		public void nested(SearchPredicate nestedPredicate) {
			LuceneSearchPredicate luceneNestedPredicate = LuceneSearchPredicate.from( scope, nestedPredicate );
			luceneNestedPredicate.checkNestableWithin( absoluteFieldPath );
			this.nestedPredicate = luceneNestedPredicate;
		}

		@Override
		public SearchPredicate build() {
			return new LuceneNestedPredicate( this );
		}
	}
}
