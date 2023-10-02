/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationExtractContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.LuceneSearchAggregation;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicate;
import org.hibernate.search.backend.lucene.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilder;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.search.Query;

public abstract class AbstractLuceneNestableAggregation<A> implements LuceneSearchAggregation<A> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String nestedDocumentPath;
	private final LuceneSearchPredicate nestedFilter;

	AbstractLuceneNestableAggregation(AbstractBuilder<A> builder) {
		this.nestedDocumentPath = builder.nestedDocumentPath;
		this.nestedFilter = builder.nestedFilter;
	}

	protected NestedDocsProvider createNestedDocsProvider(AggregationExtractContext context) {
		NestedDocsProvider nestedDocsProvider = null;
		if ( nestedDocumentPath != null ) {
			nestedDocsProvider = context.createNestedDocsProvider( nestedDocumentPath,
					toNestedFilterQuery( context.toPredicateRequestContext( nestedDocumentPath ) ) );
		}
		return nestedDocsProvider;
	}

	private Query toNestedFilterQuery(PredicateRequestContext filterContext) {
		return nestedFilter == null ? null : nestedFilter.toQuery( filterContext );
	}

	public abstract static class AbstractBuilder<A> implements SearchAggregationBuilder<A> {

		protected final LuceneSearchIndexScope<?> scope;
		protected final LuceneSearchIndexValueFieldContext<?> field;
		private final String nestedDocumentPath;
		private LuceneSearchPredicate nestedFilter;

		public AbstractBuilder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<?> field) {
			this.scope = scope;
			this.field = field;
			this.nestedDocumentPath = field.nestedDocumentPath();
		}

		public void filter(SearchPredicate filter) {
			if ( nestedDocumentPath == null ) {
				throw log.cannotFilterAggregationOnRootDocumentField( field.absolutePath(), field.eventContext() );
			}
			LuceneSearchPredicate luceneFilter = LuceneSearchPredicate.from( scope, filter );
			luceneFilter.checkNestableWithin( nestedDocumentPath );
			this.nestedFilter = luceneFilter;
		}

		@Override
		public abstract LuceneSearchAggregation<A> build();

	}
}
