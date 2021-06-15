/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.ElasticsearchSearchAggregationIndexScope;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateIndexScope;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjectionIndexScope;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortIndexScope;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.query.spi.SearchQueryIndexScope;

public interface ElasticsearchSearchQueryIndexScope<S extends ElasticsearchSearchQueryIndexScope<?>>
		extends SearchQueryIndexScope<S>,
				ElasticsearchSearchPredicateIndexScope<S>, ElasticsearchSearchSortIndexScope<S>,
				ElasticsearchSearchProjectionIndexScope<S>, ElasticsearchSearchAggregationIndexScope<S> {

	@Override
	<P> ElasticsearchSearchQueryBuilder<P> select(BackendSessionContext sessionContext,
			SearchLoadingContextBuilder<?, ?, ?> loadingContextBuilder, SearchProjection<P> projection);
}
