/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;

import org.apache.lucene.index.IndexableField;

/**
 * A projection on the values of an index field.
 *
 * @param <E> The type of the aggregated value extracted from the Lucene index (before conversion).
 * @param <P> The type of the aggregated value returned by the projection (after conversion).
 * @param <F> The type of individual field values obtained from the backend (before conversion).
 * @param <V> The type of individual field values after conversion.
 */
class LuceneFieldProjection<E, P, F, V> implements LuceneSearchProjection<E, P> {

	private final Set<String> indexNames;
	private final String absoluteFieldPath;
	private final String nestedDocumentPath;

	private final LuceneFieldCodec<F> codec;
	private final ProjectionConverter<? super F, V> converter;
	private final ProjectionAccumulator<F, V, E, P> accumulator;

	LuceneFieldProjection(Set<String> indexNames, String absoluteFieldPath, String nestedDocumentPath,
			LuceneFieldCodec<F> codec, ProjectionConverter<? super F, V> converter,
			ProjectionAccumulator<F, V, E, P> accumulator) {
		this.indexNames = indexNames;
		this.absoluteFieldPath = absoluteFieldPath;
		this.nestedDocumentPath = nestedDocumentPath;
		this.codec = codec;
		this.converter = converter;
		this.accumulator = accumulator;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "absoluteFieldPath=" + absoluteFieldPath
				+ ", accumulator=" + accumulator
				+ "]";
	}

	@Override
	public void request(SearchProjectionRequestContext context) {
		context.requireStoredField( absoluteFieldPath, nestedDocumentPath );
	}

	@Override
	public E extract(ProjectionHitMapper<?, ?> mapper, LuceneResult documentResult,
			SearchProjectionExtractContext context) {
		E extracted = accumulator.createInitial();
		for ( IndexableField field : documentResult.getDocument().getFields() ) {
			if ( field.name().equals( absoluteFieldPath ) ) {
				F decoded = codec.decode( field );
				extracted = accumulator.accumulate( extracted, decoded );
			}
		}
		return extracted;
	}

	@Override
	public P transform(LoadingResult<?> loadingResult, E extractedData,
			SearchProjectionTransformContext context) {
		FromDocumentFieldValueConvertContext convertContext = context.getFromDocumentFieldValueConvertContext();
		return accumulator.finish( extractedData, converter, convertContext );
	}

	@Override
	public Set<String> getIndexNames() {
		return indexNames;
	}
}
