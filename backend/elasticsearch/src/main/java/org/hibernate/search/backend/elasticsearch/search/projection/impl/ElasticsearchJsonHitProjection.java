/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilder;

import com.google.gson.JsonObject;

class ElasticsearchJsonHitProjection extends AbstractElasticsearchProjection<JsonObject, JsonObject> {

	private ElasticsearchJsonHitProjection(ElasticsearchSearchContext searchContext) {
		super( searchContext );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public void request(JsonObject requestBody, SearchProjectionRequestContext context) {
		// No-op
	}

	@Override
	public JsonObject extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject hit,
			SearchProjectionExtractContext context) {
		return hit;
	}

	@Override
	public JsonObject transform(LoadingResult<?, ?> loadingResult, JsonObject extractedData,
			SearchProjectionTransformContext context) {
		return extractedData;
	}

	static class Builder implements SearchProjectionBuilder<JsonObject> {

		private final ElasticsearchJsonHitProjection projection;

		Builder(ElasticsearchSearchContext searchContext) {
			this.projection = new ElasticsearchJsonHitProjection( searchContext );
		}

		@Override
		public SearchProjection<JsonObject> build() {
			return projection;
		}
	}
}
