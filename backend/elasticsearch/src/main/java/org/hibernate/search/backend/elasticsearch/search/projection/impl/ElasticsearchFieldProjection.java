/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.lang.invoke.MethodHandles;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.FieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * A projection on the values of an index field.
 *
 * @param <F> The type of individual field values obtained from the backend (before conversion).
 * @param <V> The type of individual field values after conversion.
 * @param <P> The type of the final projection result representing accumulated values of type {@code V}.
 */
public class ElasticsearchFieldProjection<F, V, P> extends AbstractElasticsearchProjection<P> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String absoluteFieldPath;
	private final String[] absoluteFieldPathComponents;
	private final String requiredContextAbsoluteFieldPath;

	private final Function<JsonElement, F> decodeFunction;
	private final ProjectionConverter<? super F, ? extends V> converter;
	private final ProjectionAccumulator.Provider<V, P> accumulatorProvider;

	private ElasticsearchFieldProjection(Builder<F, V> builder,
			ProjectionAccumulator.Provider<V, P> accumulatorProvider) {
		this( builder.scope, builder.field, builder.codec::decode, builder.converter, accumulatorProvider );
	}

	ElasticsearchFieldProjection(ElasticsearchSearchIndexScope<?> scope,
			ElasticsearchSearchIndexValueFieldContext<?> field,
			Function<JsonElement, F> decodeFunction, ProjectionConverter<? super F, ? extends V> converter,
			ProjectionAccumulator.Provider<V, P> accumulatorProvider) {
		super( scope );
		this.absoluteFieldPath = field.absolutePath();
		this.absoluteFieldPathComponents = field.absolutePathComponents();
		this.requiredContextAbsoluteFieldPath = accumulatorProvider.isSingleValued()
				? field.closestMultiValuedParentAbsolutePath()
				: null;
		this.decodeFunction = decodeFunction;
		this.converter = converter;
		this.accumulatorProvider = accumulatorProvider;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "absoluteFieldPath=" + absoluteFieldPath
				+ ", accumulatorProvider=" + accumulatorProvider
				+ "]";
	}

	@Override
	public ValueFieldExtractor<?> request(JsonObject requestBody, ProjectionRequestContext context) {
		ProjectionRequestContext innerContext = context.forField( absoluteFieldPath, absoluteFieldPathComponents );
		if ( requiredContextAbsoluteFieldPath != null
				&& !requiredContextAbsoluteFieldPath.equals( context.absoluteCurrentFieldPath() ) ) {
			throw log.invalidSingleValuedProjectionOnValueFieldInMultiValuedObjectField(
					absoluteFieldPath, requiredContextAbsoluteFieldPath );
		}
		JsonPrimitive fieldPathJson = new JsonPrimitive( absoluteFieldPath );
		AccumulatingSourceExtractor.REQUEST_SOURCE_ACCESSOR.addElementIfAbsent( requestBody, fieldPathJson );
		return new ValueFieldExtractor<>( innerContext.relativeCurrentFieldPathComponents(), accumulatorProvider.get() );
	}

	/**
	 * @param <A> The type of the temporary storage for accumulated values, before and after being transformed.
	 */
	private class ValueFieldExtractor<A> extends AccumulatingSourceExtractor<F, V, A, P> {
		public ValueFieldExtractor(String[] fieldPathComponents, ProjectionAccumulator<F, V, A, P> accumulator) {
			super( fieldPathComponents, accumulator );
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "["
					+ "absoluteFieldPath=" + absoluteFieldPath
					+ ", accumulator=" + accumulator
					+ "]";
		}

		@Override
		protected F extract(ProjectionHitMapper<?> projectionHitMapper, JsonObject hit, JsonElement sourceElement,
				ProjectionExtractContext context) {
			return decodeFunction.apply( sourceElement );
		}

		@Override
		public P transform(LoadingResult<?> loadingResult, A extractedData,
				ProjectionTransformContext context) {
			FromDocumentValueConvertContext convertContext = context.fromDocumentValueConvertContext();
			A transformedData = accumulator.transformAll( extractedData, converter, convertContext );
			return accumulator.finish( transformedData );
		}
	}

	public static class Factory<F>
			extends AbstractElasticsearchCodecAwareSearchQueryElementFactory<FieldProjectionBuilder.TypeSelector, F> {
		public Factory(ElasticsearchFieldCodec<F> codec) {
			super( codec );
		}

		@Override
		public TypeSelector<?> create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field) {
			// Check the compatibility of nested structure in the case of multi-index search.
			field.nestedPathHierarchy();
			return new TypeSelector<>( codec, scope, field );
		}
	}

	public static class TypeSelector<F> implements FieldProjectionBuilder.TypeSelector {
		private final ElasticsearchFieldCodec<F> codec;
		private final ElasticsearchSearchIndexScope<?> scope;
		private final ElasticsearchSearchIndexValueFieldContext<F> field;

		private TypeSelector(ElasticsearchFieldCodec<F> codec,
				ElasticsearchSearchIndexScope<?> scope, ElasticsearchSearchIndexValueFieldContext<F> field) {
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

	public static class Builder<F, V> implements FieldProjectionBuilder<V> {

		private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

		private final ElasticsearchFieldCodec<F> codec;

		private final ElasticsearchSearchIndexScope<?> scope;
		private final ElasticsearchSearchIndexValueFieldContext<F> field;

		private final ProjectionConverter<F, ? extends V> converter;

		private Builder(ElasticsearchFieldCodec<F> codec, ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field, ProjectionConverter<F, ? extends V> converter) {
			this.codec = codec;
			this.scope = scope;
			this.field = field;
			this.converter = converter;
		}

		@Override
		public <P> SearchProjection<P> build(ProjectionAccumulator.Provider<V, P> accumulatorProvider) {
			if ( accumulatorProvider.isSingleValued() && field.multiValued() ) {
				throw log.invalidSingleValuedProjectionOnMultiValuedField( field.absolutePath(), field.eventContext() );
			}
			return new ElasticsearchFieldProjection<>( this, accumulatorProvider );
		}
	}
}
