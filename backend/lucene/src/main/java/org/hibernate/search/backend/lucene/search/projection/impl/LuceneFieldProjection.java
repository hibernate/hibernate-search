/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.lang.invoke.MethodHandles;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.common.ValueConvert;
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

	private final Function<IndexableField, F> decodeFunction;
	private final ProjectionConverter<F, ? extends V> converter;
	private final ProjectionAccumulator<F, V, E, P> accumulator;

	private LuceneFieldProjection(Builder<F, V> builder, ProjectionAccumulator<F, V, E, P> accumulator) {
		this( builder.scope, builder.field, builder.codec::decode, builder.converter, accumulator );
	}

	LuceneFieldProjection(LuceneSearchIndexScope<?> scope,
			LuceneSearchIndexValueFieldContext<?> field,
			Function<IndexableField, F> decodeFunction, ProjectionConverter<F, ? extends V> converter,
			ProjectionAccumulator<F, V, E, P> accumulator) {
		super( scope );
		this.absoluteFieldPath = field.absolutePath();
		this.nestedDocumentPath = field.nestedDocumentPath();
		this.decodeFunction = decodeFunction;
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
	public void request(ProjectionRequestContext context) {
		context.requireStoredField( absoluteFieldPath, nestedDocumentPath );
	}

	@Override
	public E extract(ProjectionHitMapper<?, ?> mapper, LuceneResult documentResult,
			ProjectionExtractContext context) {
		E extracted = accumulator.createInitial();
		for ( IndexableField field : documentResult.getDocument().getFields() ) {
			if ( field.name().equals( absoluteFieldPath ) ) {
				F decoded = decodeFunction.apply( field );
				extracted = accumulator.accumulate( extracted, decoded );
			}
		}
		return extracted;
	}

	@Override
	public P transform(LoadingResult<?, ?> loadingResult, E extractedData,
			ProjectionTransformContext context) {
		FromDocumentValueConvertContext convertContext = context.fromDocumentValueConvertContext();
		return accumulator.finish( extractedData, converter, convertContext );
	}

	public static class Factory<F>
			extends
			AbstractLuceneCodecAwareSearchQueryElementFactory<FieldProjectionBuilder.TypeSelector, F, LuceneFieldCodec<F>> {
		public Factory(LuceneFieldCodec<F> codec) {
			super( codec );
		}

		@Override
		public TypeSelector<?> create(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			// Fail early if the nested structure differs in the case of multi-index search.
			field.nestedPathHierarchy();
			return new TypeSelector<>( codec, scope, field );
		}
	}

	private static class TypeSelector<F> implements FieldProjectionBuilder.TypeSelector {
		private final LuceneFieldCodec<F> codec;
		private final LuceneSearchIndexScope<?> scope;
		private final LuceneSearchIndexValueFieldContext<F> field;

		private TypeSelector(LuceneFieldCodec<F> codec,
				LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			this.codec = codec;
			this.scope = scope;
			this.field = field;
		}

		@Override
		public <V> Builder<F, V> type(Class<V> expectedType, ValueConvert convert) {
			return new Builder<>( codec, scope, field,
					field.type().projectionConverter( convert ).withConvertedType( expectedType, field ) );
		}
	}

	private static class Builder<F, V> extends AbstractLuceneProjection.AbstractBuilder<V>
			implements FieldProjectionBuilder<V> {

		private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

		private final LuceneFieldCodec<F> codec;

		private final LuceneSearchIndexValueFieldContext<F> field;

		private final ProjectionConverter<F, ? extends V> converter;

		private Builder(LuceneFieldCodec<F> codec, LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<F> field, ProjectionConverter<F, ? extends V> converter) {
			super( scope );
			this.codec = codec;
			this.field = field;
			this.converter = converter;
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
