/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

import com.google.gson.JsonObject;

class ElasticsearchExplanationProjection implements ElasticsearchSearchProjection<JsonObject, JsonObject> {

	private static final JsonAccessor<Boolean> REQUEST_EXPLAIN_ACCESSOR = JsonAccessor.root().property( "explain" ).asBoolean();
	private static final JsonObjectAccessor HIT_EXPLANATION_ACCESSOR = JsonAccessor.root().property( "_explanation" ).asObject();

	private final Set<String> indexNames;

	ElasticsearchExplanationProjection(Set<String> indexNames) {
		this.indexNames = indexNames;
	}

	@Override
	public void request(JsonObject requestBody, SearchProjectionRequestContext context) {
		REQUEST_EXPLAIN_ACCESSOR.set( requestBody, true );
	}

	@Override
	public JsonObject extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject responseBody, JsonObject hit,
			SearchProjectionExtractContext context) {
		// We expect the optional to always be non-empty.
		return HIT_EXPLANATION_ACCESSOR.get( hit ).get();
	}

	@Override
	public JsonObject transform(LoadingResult<?> loadingResult, JsonObject extractedData,
			SearchProjectionTransformContext context) {
		return extractedData;
	}

	@Override
	public Set<String> getIndexNames() {
		return indexNames;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

}
