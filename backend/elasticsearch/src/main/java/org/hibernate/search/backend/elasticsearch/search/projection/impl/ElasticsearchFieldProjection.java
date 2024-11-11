/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.ProjectionCollector;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.FieldProjectionBuilder;
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
public class ElasticsearchFieldProjection<F, V, P, T> extends AbstractElasticsearchProjection<P> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String absoluteFieldPath;
	private final String[] absoluteFieldPathComponents;
	private final String requiredContextAbsoluteFieldPath;

	private final Function<JsonElement, T> decodeFunction;
	private final boolean canDecodeArrays;
	private final ProjectionConverter<? super T, ? extends V> converter;
	private final ProjectionCollector.Provider<V, P> collectorProvider;

	private ElasticsearchFieldProjection(Builder<F, V, T> builder,
			ProjectionCollector.Provider<V, P> collectorProvider) {
		this( builder.scope, builder.field, builder.decodeFunction, builder.canDecodeArrays, builder.converter,
				collectorProvider
		);
	}

	ElasticsearchFieldProjection(ElasticsearchSearchIndexScope<?> scope,
			ElasticsearchSearchIndexValueFieldContext<?> field,
			Function<JsonElement, T> decodeFunction, boolean canDecodeArrays,
			ProjectionConverter<? super T, ? extends V> converter,
			ProjectionCollector.Provider<V, P> collectorProvider) {
		super( scope );
		this.absoluteFieldPath = field.absolutePath();
		this.absoluteFieldPathComponents = field.absolutePathComponents();
		this.requiredContextAbsoluteFieldPath = collectorProvider.isSingleValued()
				? field.closestMultiValuedParentAbsolutePath()
				: null;
		this.decodeFunction = decodeFunction;
		this.canDecodeArrays = canDecodeArrays;
		this.converter = converter;
		this.collectorProvider = collectorProvider;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "absoluteFieldPath=" + absoluteFieldPath
				+ ", collectorProvider=" + collectorProvider
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
		return new ValueFieldExtractor<>( innerContext.relativeCurrentFieldPathComponents(), collectorProvider.get() );
	}

	/**
	 * @param <A> The type of the temporary storage for accumulated values, before and after being transformed.
	 */
	private class ValueFieldExtractor<A> extends AccumulatingSourceExtractor<T, V, A, P> {
		public ValueFieldExtractor(String[] fieldPathComponents, ProjectionCollector<T, V, A, P> collector) {
			super( fieldPathComponents, collector );
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "["
					+ "absoluteFieldPath=" + absoluteFieldPath
					+ ", collector=" + collector
					+ "]";
		}

		@Override
		protected T extract(ProjectionHitMapper<?> projectionHitMapper, JsonObject hit, JsonElement sourceElement,
				ProjectionExtractContext context) {
			return decodeFunction.apply( sourceElement );
		}

		@Override
		protected boolean canDecodeArrays() {
			return canDecodeArrays;
		}

		@Override
		public P transform(LoadingResult<?> loadingResult, A extractedData,
				ProjectionTransformContext context) {
			FromDocumentValueConvertContext convertContext = context.fromDocumentValueConvertContext();
			A transformedData = collector.transformAll( extractedData, converter.delegate(), convertContext );
			return collector.finish( transformedData );
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

		@SuppressWarnings("unchecked")
		@Override
		public <V> Builder<F, V, ?> type(Class<V> expectedType, ValueModel valueModel) {
			if ( ValueModel.RAW.equals( valueModel ) ) {
				return new Builder<>( Function.identity(), codec.canDecodeArrays(), scope, field,
						// unchecked cast to make eclipse-compiler happy
						// we know that Elasticsearch projection converters work with the JsonElement
						( (ProjectionConverter<JsonElement, ?>) field.type().rawProjectionConverter() )
								.withConvertedType( expectedType, field )
				);
			}
			else {
				return new Builder<>( codec::decode, codec.canDecodeArrays(), scope, field,
						field.type().projectionConverter( valueModel ).withConvertedType( expectedType, field )
				);
			}
		}
	}

	public static class Builder<F, V, T> implements FieldProjectionBuilder<V> {

		private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

		private final Function<JsonElement, T> decodeFunction;
		private final boolean canDecodeArrays;
		private final ElasticsearchSearchIndexScope<?> scope;
		private final ElasticsearchSearchIndexValueFieldContext<F> field;
		private final ProjectionConverter<T, ? extends V> converter;

		private Builder(Function<JsonElement, T> decodeFunction, boolean canDecodeArrays,
				ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field, ProjectionConverter<T, ? extends V> converter) {
			this.decodeFunction = decodeFunction;
			this.canDecodeArrays = canDecodeArrays;
			this.scope = scope;
			this.field = field;
			this.converter = converter;
		}

		@Override
		public <P> SearchProjection<P> build(ProjectionCollector.Provider<V, P> collectorProvider) {
			if ( collectorProvider.isSingleValued() && field.multiValued() ) {
				throw log.invalidSingleValuedProjectionOnMultiValuedField( field.absolutePath(), field.eventContext() );
			}
			return new ElasticsearchFieldProjection<>( this, collectorProvider );
		}
	}
}
