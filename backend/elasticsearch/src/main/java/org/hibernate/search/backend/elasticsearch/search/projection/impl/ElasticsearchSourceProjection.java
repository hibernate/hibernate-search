/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonArrayAccessor;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

class ElasticsearchSourceProjection extends AbstractElasticsearchProjection<JsonObject>
		implements ElasticsearchSearchProjection.Extractor<JsonObject, JsonObject> {

	private static final JsonArrayAccessor REQUEST_SOURCE_ACCESSOR = JsonAccessor.root().property( "_source" ).asArray();
	private static final JsonPrimitive WILDCARD_ALL = new JsonPrimitive( "*" );

	private ElasticsearchSourceProjection(ElasticsearchSearchIndexScope<?> scope) {
		super( scope );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public Extractor<?, JsonObject> request(JsonObject requestBody, ProjectionRequestContext context) {
		REQUEST_SOURCE_ACCESSOR.addElementIfAbsent( requestBody, WILDCARD_ALL );
		return this;
	}

	@Override
	public JsonObject extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject hit,
			JsonObject source, ProjectionExtractContext context) {
		return source;
	}

	@Override
	public JsonObject transform(LoadingResult<?, ?> loadingResult, JsonObject extractedData,
			ProjectionTransformContext context) {
		return extractedData;
	}

	static class Builder extends AbstractElasticsearchProjection.AbstractBuilder<JsonObject> {

		private final ElasticsearchSourceProjection projection;

		Builder(ElasticsearchSearchIndexScope<?> scope) {
			super( scope );
			this.projection = new ElasticsearchSourceProjection( scope );
		}

		@Override
		public SearchProjection<JsonObject> build() {
			return projection;
		}
	}
}
