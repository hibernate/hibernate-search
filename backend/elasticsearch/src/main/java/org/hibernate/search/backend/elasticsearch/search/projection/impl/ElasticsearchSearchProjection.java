/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

public interface ElasticsearchSearchProjection<E, P> extends SearchProjection<P> {

	Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	Set<String> indexNames();

	/**
	 * Contribute to the request, making sure that the requirements for this projection are met.
	 * @param requestBody The request body.
	 * @param context An execution context that will share state with the context passed to
	 * {@link #extract(ProjectionHitMapper, JsonObject, SearchProjectionExtractContext)}.
	 */
	void request(JsonObject requestBody, SearchProjectionRequestContext context);

	/**
	 * Perform hit extraction.
	 * <p>
	 * Implementations should only perform operations relative to extracting content from the index,
	 * delaying operations that rely on the mapper until
	 * {@link #transform(LoadingResult, Object, SearchProjectionTransformContext)} is called,
	 * so that blocking mapper operations (if any) do not pollute backend threads.
	 *
	 * @param projectionHitMapper The projection hit mapper used to transform hits to entities.
	 * @param hit The part of the response body relevant to the hit to extract.
	 * @param context An execution context for the extraction.
	 * @return The element extracted from the hit. Might be a key referring to an object that will be loaded by the
	 * {@link ProjectionHitMapper}. This returned object will be passed to {@link #transform(LoadingResult, Object, SearchProjectionTransformContext)}.
	 */
	E extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject hit,
			SearchProjectionExtractContext context);

	/**
	 * Transform the extracted data to the actual projection result.
	 *
	 * @param loadingResult Container containing all the entities that have been loaded by the
	 * {@link ProjectionHitMapper}.
	 * @param extractedData The extracted data to transform, coming from the
	 * {@link #extract(ProjectionHitMapper, JsonObject, SearchProjectionExtractContext)} method.
	 * @param context An execution context for the transforming.
	 * @return The final result considered as a hit.
	 */
	P transform(LoadingResult<?, ?> loadingResult, E extractedData, SearchProjectionTransformContext context);

	static <P> ElasticsearchSearchProjection<?, P> from(ElasticsearchSearchIndexScope<?> scope, SearchProjection<P> projection) {
		if ( !( projection instanceof ElasticsearchSearchProjection ) ) {
			throw log.cannotMixElasticsearchSearchQueryWithOtherProjections( projection );
		}
		@SuppressWarnings("unchecked") // Necessary for ecj (Eclipse compiler)
		ElasticsearchSearchProjection<?, P> casted = (ElasticsearchSearchProjection<?, P>) projection;
		if ( !scope.hibernateSearchIndexNames().equals( casted.indexNames() ) ) {
			throw log.projectionDefinedOnDifferentIndexes( projection, casted.indexNames(),
					scope.hibernateSearchIndexNames() );
		}
		return casted;
	}

	/**
	 * Transform the extracted data and cast it to the right type.
	 * <p>
	 * This should be used with care as it's unsafe.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	static <Z> Z transformUnsafe(ElasticsearchSearchProjection<?, Z> projection, LoadingResult<?, ?> loadingResult,
			Object extractedData, SearchProjectionTransformContext context) {
		return (Z) ( (ElasticsearchSearchProjection) projection ).transform( loadingResult, extractedData, context );
	}
}
