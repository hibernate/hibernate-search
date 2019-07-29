/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.search.projection.impl.SearchProjectionExtractContext;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;

import com.google.gson.JsonObject;

/**
 * The context holding all the useful information pertaining to the Elasticsearch search query,
 * to be used when extracting data from the response,
 * to get an "extract" context linked to the session/loading context
 * ({@link #createExtractContext(JsonObject)}.
 */
class ElasticsearchSearchQueryRequestContext {

	private final SessionContextImplementor sessionContext;
	private final LoadingContext<?, ?> loadingContext;
	private final Map<SearchProjectionExtractContext.DistanceSortKey, Integer> distanceSorts;

	ElasticsearchSearchQueryRequestContext(
			SessionContextImplementor sessionContext,
			LoadingContext<?, ?> loadingContext,
			Map<SearchProjectionExtractContext.DistanceSortKey, Integer> distanceSorts) {
		this.sessionContext = sessionContext;
		this.loadingContext = loadingContext;
		this.distanceSorts = distanceSorts;
	}

	ElasticsearchSearchQueryExtractContext createExtractContext(JsonObject responseBody) {
		return new ElasticsearchSearchQueryExtractContext(
				sessionContext,
				loadingContext.getProjectionHitMapper(),
				distanceSorts,
				responseBody
		);
	}

}
