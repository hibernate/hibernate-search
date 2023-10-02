/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import static org.hibernate.search.util.common.impl.CollectionHelper.isSubset;
import static org.hibernate.search.util.common.impl.CollectionHelper.notInTheOtherSet;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.TopDocsDataCollector;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.Values;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public interface LuceneSearchProjection<P> extends SearchProjection<P> {

	Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	Set<String> indexNames();

	/**
	 * Request the collection of per-document data that will be used in
	 * {@link Extractor#values(ProjectionExtractContext)},
	 * making sure that the requirements for this projection are met.
	 *
	 * @param context An execution context for the request.
	 * @return An {@link Extractor}, to extract the result of the projection from the Elasticsearch response.
	 */
	Extractor<?, P> request(ProjectionRequestContext context);

	/**
	 * An object responsible for extracting data from the Lucene Searcher,
	 * to implement a projection.
	 *
	 * @param <E> The type of temporary values extracted from the response. May be the same as {@link P}, or not,
	 * depending on implementation.
	 * @param <P> The type of projected values.
	 */
	interface Extractor<E, P> {
		/**
		 * Creates low-level values for use in a {@link TopDocsDataCollector}.
		 * <p>
		 * The returned {@link Values} should only perform operations relative to extracting content from the index,
		 * delaying operations that rely on the mapper until
		 * {@link #transform(LoadingResult, Object, ProjectionTransformContext)} is called,
		 * so that blocking mapper operations (if any) do not pollute backend threads.
		 * @param context An execution context for the extraction.
		 * @return The {@link Values} to use during Lucene collection of top docs data.
		 */
		Values<E> values(ProjectionExtractContext context);

		/**
		 * Transforms the extracted data to the actual projection result.
		 *
		 * @param loadingResult Container containing all the entities that have been loaded by the
		 * {@link ProjectionHitMapper}.
		 * @param extractedData The extracted data to transform, returned by the
		 * {@link #values(ProjectionExtractContext) value source}.
		 * @param context An execution context for the transforming.
		 * @return The final result considered as a hit.
		 */
		P transform(LoadingResult<?> loadingResult, E extractedData,
				ProjectionTransformContext context);

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

	static <P> LuceneSearchProjection<P> from(LuceneSearchIndexScope<?> scope, SearchProjection<P> projection) {
		if ( !( projection instanceof LuceneSearchProjection ) ) {
			throw log.cannotMixLuceneSearchQueryWithOtherProjections( projection );
		}
		@SuppressWarnings("unchecked") // Necessary for ecj (Eclipse compiler)
		LuceneSearchProjection<P> casted = (LuceneSearchProjection<P>) projection;
		if ( !isSubset( scope.hibernateSearchIndexNames(), casted.indexNames() ) ) {
			throw log.projectionDefinedOnDifferentIndexes( projection, casted.indexNames(), scope.hibernateSearchIndexNames(),
					notInTheOtherSet( scope.hibernateSearchIndexNames(), casted.indexNames() ) );
		}
		return casted;
	}

}
