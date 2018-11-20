/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.query.spi.LoadingResult;
import org.hibernate.search.engine.search.query.spi.ProjectionHitMapper;

import com.google.gson.JsonObject;

public interface ElasticsearchSearchProjection<E, T> extends SearchProjection<T> {

	/**
	 * Contribute to the request, making sure that the requirements for this projection are met.
	 *
	 * @param requestBody The request body.
	 * @param searchProjectionExecutionContext An execution context for the search projections.
	 */
	void contributeRequest(JsonObject requestBody, SearchProjectionExecutionContext searchProjectionExecutionContext);

	/**
	 * Perform hit extraction.
	 *
	 * @param projectionHitMapper The projection hit mapper used to transform hits to entities.
	 * @param responseBody The full body of the response.
	 * @param hit The part of the response body relevant to the hit to extract.
	 * @param searchProjectionExecutionContext An execution context for the search projections.
	 * @return The element extracted from the hit. Might be a key referring to an object that will be loaded by the
	 * {@link ProjectionHitMapper}. This returned object will be passed to {@link #transform(LoadingResult, Object)}.
	 */
	E extract(ProjectionHitMapper<?, ?> projectionHitMapper,
			JsonObject responseBody, JsonObject hit,
			SearchProjectionExecutionContext searchProjectionExecutionContext);

	/**
	 * Transform the extracted data to the actual projection result.
	 *
	 * @param loadingResult Container containing all the entities that have been loaded by the
	 * {@link ProjectionHitMapper}.
	 * @param extractedData The extracted data to transform, coming from the
	 * {@link #extract(ProjectionHitMapper, JsonObject, JsonObject, SearchProjectionExecutionContext)} method.
	 * @return The final result considered as a hit.
	 */
	T transform(LoadingResult<?> loadingResult, E extractedData);

	/**
	 * Transform the extracted data and cast it to the right type.
	 * <p>
	 * This should be used with care as it's unsafe.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	static <Z> Z transformUnsafe(ElasticsearchSearchProjection<?, Z> projection, LoadingResult<?> loadingResult,
			Object extractedData) {
		return (Z) ( (ElasticsearchSearchProjection) projection ).transform( loadingResult, extractedData );
	}
}
