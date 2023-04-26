/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.backend.lucene.search.aggregation.dsl.LuceneSearchAggregationFactory;
import org.hibernate.search.backend.lucene.search.aggregation.impl.LuceneSearchAggregationIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.predicate.dsl.LuceneSearchPredicateFactory;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateIndexScope;
import org.hibernate.search.backend.lucene.search.projection.dsl.LuceneSearchProjectionFactory;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjectionIndexScope;
import org.hibernate.search.backend.lucene.search.sort.dsl.LuceneSearchSortFactory;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortIndexScope;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.query.spi.SearchQueryIndexScope;
import org.hibernate.search.engine.search.timeout.spi.TimeoutManager;

import org.apache.lucene.search.Query;

public interface LuceneSearchQueryIndexScope<S extends LuceneSearchQueryIndexScope<?>>
		extends SearchQueryIndexScope<S>, LuceneSearchIndexScope<S>,
				LuceneSearchPredicateIndexScope<S>, LuceneSearchSortIndexScope<S>,
				LuceneSearchProjectionIndexScope<S>, LuceneSearchAggregationIndexScope<S> {

	@Override
	<P> LuceneSearchQueryBuilder<P> select(BackendSessionContext sessionContext,
			SearchLoadingContextBuilder<?, ?> loadingContextBuilder, SearchProjection<P> projection);

	@Override
	LuceneSearchPredicateFactory predicateFactory();

	@Override
	LuceneSearchSortFactory sortFactory();

	@Override
	<R, E> LuceneSearchProjectionFactory<R, E> projectionFactory();

	@Override
	LuceneSearchAggregationFactory aggregationFactory();

	Query filterOrNull(String tenantId);

	TimeoutManager createTimeoutManager(Long timeout, TimeUnit timeUnit, boolean exceptionOnTimeout);

	Collection<? extends LuceneSearchIndexContext> indexes();

	Map<String, ? extends LuceneSearchIndexContext> mappedTypeNameToIndex();

	boolean hasNestedDocuments();

}
