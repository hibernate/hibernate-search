/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.SearchProjectionExtractContext;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;

import com.google.gson.JsonObject;

/**
 * A search result extractor for ES6.
 * <p>
 * Compared to ES7:
 * <ul>
 *     <li>The total hit count is retrieved from hits.total instead of hits.total.value</li>
 * </ul>
 */
class Elasticsearch6SearchResultExtractor<T> extends Elasticsearch7SearchResultExtractor<T> {

	private static final JsonAccessor<Long> HITS_TOTAL_ACCESSOR =
			HITS_ACCESSOR.property( "total" ).asLong();

	Elasticsearch6SearchResultExtractor(
			LoadingContext<?, ?> loadingContext,
			ElasticsearchSearchProjection<?, T> rootProjection,
			SearchProjectionExtractContext searchProjectionExecutionContext) {
		super( loadingContext, rootProjection, searchProjectionExecutionContext );
	}

	@Override
	protected long extractHitCount(JsonObject responseBody) {
		return HITS_TOTAL_ACCESSOR.get( responseBody ).orElse( 0L );
	}

}
