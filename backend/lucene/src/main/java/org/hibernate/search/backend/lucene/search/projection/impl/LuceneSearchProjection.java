/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public interface LuceneSearchProjection<E, P> extends SearchProjection<P> {

	Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	Set<String> indexNames();

	/**
	 * Request the collection of per-document data that will be used in
	 * {@link #extract(ProjectionHitMapper, LuceneResult, SearchProjectionExtractContext)},
	 * making sure that the requirements for this projection are met.
	 *
	 * @param context A context that will share its state with the context passed to
	 * {@link #extract(ProjectionHitMapper, LuceneResult, SearchProjectionExtractContext)} .
	 */
	void request(SearchProjectionRequestContext context);

	/**
	 * Perform hit extraction.
	 * <p>
	 * Implementations should only perform operations relative to extracting content from the index,
	 * delaying operations that rely on the mapper until
	 * {@link #transform(LoadingResult, Object, SearchProjectionTransformContext)} is called,
	 * so that blocking mapper operations (if any) do not pollute backend threads.
	 *
	 * @param projectionHitMapper The projection hit mapper used to transform hits to entities.
	 * @param luceneResult A wrapper on top of the Lucene document extracted from the index.
	 * @param context An execution context for the extraction.
	 * @return The element extracted from the hit. Might be a key referring to an object that will be loaded by the
	 * {@link ProjectionHitMapper}. This returned object will be passed to {@link #transform(LoadingResult, Object, SearchProjectionTransformContext)}.
	 */
	E extract(ProjectionHitMapper<?, ?> projectionHitMapper, LuceneResult luceneResult,
			SearchProjectionExtractContext context);

	/**
	 * Transform the extracted data to the actual projection result.
	 *
	 * @param loadingResult Container containing all the entities that have been loaded by the
	 * {@link ProjectionHitMapper}.
	 * @param extractedData The extracted data to transform, coming from the
	 * {@link #extract(ProjectionHitMapper, LuceneResult, SearchProjectionExtractContext)} method.
	 * @param context An execution context for the transforming.
	 * @return The final result considered as a hit.
	 */
	P transform(LoadingResult<?, ?> loadingResult, E extractedData,
			SearchProjectionTransformContext context);

	static <P> LuceneSearchProjection<?, P> from(LuceneSearchContext searchContext, SearchProjection<P> projection) {
		if ( !( projection instanceof LuceneSearchProjection ) ) {
			throw log.cannotMixLuceneSearchQueryWithOtherProjections( projection );
		}
		LuceneSearchProjection<?, P> casted = (LuceneSearchProjection<?, P>) projection;
		if ( !searchContext.indexes().indexNames().equals( casted.indexNames() ) ) {
			throw log.projectionDefinedOnDifferentIndexes( projection, casted.indexNames(),
					searchContext.indexes().indexNames() );
		}
		return casted;
	}

	/**
	 * Transform the extracted data and cast it to the right type.
	 * <p>
	 * This should be used with care as it's unsafe.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	static <Z> Z transformUnsafe(LuceneSearchProjection<?, Z> projection, LoadingResult<?, ?> loadingResult,
			Object extractedData, SearchProjectionTransformContext context) {
		return (Z) ( (LuceneSearchProjection) projection ).transform( loadingResult, extractedData, context );
	}
}
