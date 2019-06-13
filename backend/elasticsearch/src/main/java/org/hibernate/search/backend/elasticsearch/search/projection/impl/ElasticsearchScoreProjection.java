/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

import com.google.gson.JsonObject;

class ElasticsearchScoreProjection implements ElasticsearchSearchProjection<Float, Float> {

	private static final JsonAccessor<Boolean> TRACK_SCORES_ACCESSOR = JsonAccessor.root().property( "track_scores" )
			.asBoolean();

	private final Set<String> indexNames;

	ElasticsearchScoreProjection(Set<String> indexNames) {
		this.indexNames = indexNames;
	}

	@Override
	public void contributeRequest(JsonObject requestBody, SearchProjectionExtractContext context) {
		TRACK_SCORES_ACCESSOR.set( requestBody, true );
	}

	@Override
	public Float extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject responseBody, JsonObject hit,
			SearchProjectionExtractContext context) {
		return hit.get( "_score" ).getAsFloat();
	}

	@Override
	public Float transform(LoadingResult<?> loadingResult, Float extractedData,
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
