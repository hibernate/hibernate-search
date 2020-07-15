/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.FieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.index.IndexableField;

/**
 * A projection on the values of an index field.
 *
 * @param <E> The type of the aggregated value extracted from the Lucene index (before conversion).
 * @param <P> The type of the aggregated value returned by the projection (after conversion).
 * @param <F> The type of individual field values obtained from the backend (before conversion).
 * @param <V> The type of individual field values after conversion.
 */
public class LuceneFieldProjection<E, P, F, V> extends AbstractLuceneProjection<E, P> {

	private final String absoluteFieldPath;
	private final String nestedDocumentPath;

	private final LuceneFieldCodec<F> codec;
	private final ProjectionConverter<F, ? extends V> converter;
	private final ProjectionAccumulator<F, V, E, P> accumulator;

	private LuceneFieldProjection(Builder<F, V> builder, ProjectionAccumulator<F, V, E, P> accumulator) {
		super( builder );
		this.absoluteFieldPath = builder.field.absolutePath();
		this.nestedDocumentPath = builder.field.nestedDocumentPath();
		this.codec = builder.codec;
		this.converter = builder.converter;
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

	public static class Builder<F, V> extends AbstractLuceneProjection.AbstractBuilder<V>
			implements FieldProjectionBuilder<V> {

		private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

		private final LuceneSearchFieldContext<F> field;

		private final ProjectionConverter<F, ? extends V> converter;
		private final LuceneFieldCodec<F> codec;

		public Builder(LuceneSearchContext searchContext, LuceneSearchFieldContext<F> field,
				ProjectionConverter<F, ? extends V> converter, LuceneFieldCodec<F> codec) {
			super( searchContext );
			this.field = field;
			this.converter = converter;
			this.codec = codec;
		}

		@Override
		public <P> SearchProjection<P> build(ProjectionAccumulator.Provider<V, P> accumulatorProvider) {
			if ( accumulatorProvider.isSingleValued() && field.multiValuedInRoot() ) {
				throw log.invalidSingleValuedProjectionOnMultiValuedField( field.absolutePath(), field.eventContext() );
			}
			return new LuceneFieldProjection<>( this, accumulatorProvider.get() );
		}
	}
}
