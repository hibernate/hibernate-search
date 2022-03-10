/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilder;

import com.google.gson.JsonObject;

class ElasticsearchJsonHitProjection extends AbstractElasticsearchProjection<JsonObject, JsonObject> {

	private ElasticsearchJsonHitProjection(ElasticsearchSearchIndexScope<?> scope) {
		super( scope );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public void request(JsonObject requestBody, ProjectionRequestContext context) {
		// No-op
	}

	@Override
	public JsonObject extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject hit,
			ProjectionExtractContext context) {
		return hit;
	}

	@Override
	public JsonObject transform(LoadingResult<?, ?> loadingResult, JsonObject extractedData,
			ProjectionTransformContext context) {
		return extractedData;
	}

	static class Builder implements SearchProjectionBuilder<JsonObject> {

		private final ElasticsearchJsonHitProjection projection;

		Builder(ElasticsearchSearchIndexScope<?> scope) {
			this.projection = new ElasticsearchJsonHitProjection( scope );
		}

		@Override
		public SearchProjection<JsonObject> build() {
			return projection;
		}
	}
}
