/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilderFactory;

public class ElasticsearchSearchQueryBuilderFactory
		implements SearchQueryBuilderFactory {

	private final SearchBackendContext searchBackendContext;

	private final ElasticsearchSearchIndexScope scope;

	public ElasticsearchSearchQueryBuilderFactory(SearchBackendContext searchBackendContext,
			ElasticsearchSearchIndexScope scope) {
		this.searchBackendContext = searchBackendContext;
		this.scope = scope;
	}

	@Override
	public <P> ElasticsearchSearchQueryBuilder<P> select(
			BackendSessionContext sessionContext, SearchLoadingContextBuilder<?, ?, ?> loadingContextBuilder,
			SearchProjection<P> projection) {
		return searchBackendContext.createSearchQueryBuilder(
				scope,
				sessionContext,
				loadingContextBuilder, ElasticsearchSearchProjection.from( scope, projection )
		);
	}
}
