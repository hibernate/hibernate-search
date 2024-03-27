/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import static org.hibernate.search.util.common.impl.CollectionHelper.isSubset;
import static org.hibernate.search.util.common.impl.CollectionHelper.notInTheOtherSet;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

public interface ElasticsearchSearchProjection<P> extends SearchProjection<P> {

	Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	Set<String> indexNames();

	/**
	 * Contributes to the request, making sure that the requirements for this projection are met,
	 * and creating an {@link Extractor} that will be able to extract the result of the projection
	 * from the Elasticsearch response.
	 *
	 * @param requestBody The request body.
	 * @param context An execution context for the request.
	 * @return An {@link Extractor}, to extract the result of the projection from the Elasticsearch response.
	 */
	Extractor<?, P> request(JsonObject requestBody, ProjectionRequestContext context);

	/**
	 * An object responsible for extracting data from the Elasticsearch response,
	 * to implement a projection.
	 *
	 * @param <E> The type of temporary values extracted from the response. May be the same as {@link P}, or not,
	 * depending on implementation.
	 * @param <P> The type of projected values.
	 */
	interface Extractor<E, P> {

		/**
		 * Performs hit extraction.
		 * <p>
		 * Implementations should only perform operations relative to extracting content from the index,
		 * delaying operations that rely on the mapper until
		 * {@link #transform(LoadingResult, Object, ProjectionTransformContext)} is called,
		 * so that blocking mapper operations (if any) do not pollute backend threads.
		 *
		 * @param projectionHitMapper The projection hit mapper used to transform hits to entities.
		 * @param hit The part of the response body relevant to the hit to extract.
		 * @param source The part of the source that this extractor should extract from (if relevant).
		 * @param context An execution context for the extraction.
		 * @return The element extracted from the hit. Might be a key referring to an object that will be loaded by the
		 * {@link ProjectionHitMapper}.
		 * This returned object will be passed to {@link #transform(LoadingResult, Object, ProjectionTransformContext)}.
		 */
		E extract(ProjectionHitMapper<?> projectionHitMapper, JsonObject hit,
				JsonObject source, ProjectionExtractContext context);

		/**
		 * Transforms the extracted data to the actual projection result.
		 *
		 * @param loadingResult Container containing all the entities that have been loaded by the
		 * {@link ProjectionHitMapper}.
		 * @param extractedData The extracted data to transform, coming from the
		 * {@link #extract(ProjectionHitMapper, JsonObject, JsonObject, ProjectionExtractContext)} method.
		 * @param context An execution context for the transforming.
		 * @return The final result considered as a hit.
		 */
		P transform(LoadingResult<?> loadingResult, E extractedData, ProjectionTransformContext context);

		/**
		 * Transforms the extracted data and casts it to the right type.
		 * <p>
		 * This should be used with care as it's unsafe.
		 */
		@SuppressWarnings({ "unchecked", "rawtypes" })
		static <Z> Z transformUnsafe(Extractor<?, Z> extractor, LoadingResult<?> loadingResult,
				Object extractedData, ProjectionTransformContext context) {
			return (Z) ( (Extractor) extractor ).transform( loadingResult, extractedData, context );
		}
	}

	static <P> ElasticsearchSearchProjection<P> from(ElasticsearchSearchIndexScope<?> scope, SearchProjection<P> projection) {
		if ( !( projection instanceof ElasticsearchSearchProjection ) ) {
			throw log.cannotMixElasticsearchSearchQueryWithOtherProjections( projection );
		}
		@SuppressWarnings("unchecked") // Necessary for ecj (Eclipse compiler)
		ElasticsearchSearchProjection<P> casted = (ElasticsearchSearchProjection<P>) projection;
		if ( !isSubset( scope.hibernateSearchIndexNames(), casted.indexNames() ) ) {
			throw log.projectionDefinedOnDifferentIndexes( projection, casted.indexNames(), scope.hibernateSearchIndexNames(),
					notInTheOtherSet( scope.hibernateSearchIndexNames(), casted.indexNames() ) );
		}
		return casted;
	}

}
