/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import static org.hibernate.search.backend.elasticsearch.search.aggregation.impl.AggregationRequestBuildingContextContext.buildingContextKey;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicate;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.util.common.impl.CollectionHelper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @param <F> The type of field values.
 * @param <K> The type of keys in the returned map. It can be {@code F}
 * or a different type if value converters are used.
 */
public class ElasticsearchTermsAggregation<F, K, T, V>
		extends AbstractElasticsearchBucketAggregation<K, V> {

	private final String absoluteFieldPath;

	private final ProjectionConverter<T, ? extends K> fromFieldValueConverter;
	private final BiFunction<JsonElement, JsonElement, T> decodeFunction;

	private final ElasticsearchSearchAggregation<V> aggregation;

	private final JsonObject order;
	private final int size;
	private final int minDocCount;

	private ElasticsearchTermsAggregation(Builder<F, K, T, V> builder) {
		super( builder );
		this.absoluteFieldPath = builder.field.absolutePath();
		this.fromFieldValueConverter = builder.fromFieldValueConverter;
		this.decodeFunction = builder.decodeFunction;
		this.order = builder.order;
		this.size = builder.size;
		this.minDocCount = builder.minDocCount;
		this.aggregation = builder.aggregation;
	}

	@Override
	protected void doRequest(JsonObject outerObject, JsonObject innerObject, AggregationRequestBuildingContextContext context) {
		outerObject.add( "terms", innerObject );
		innerObject.addProperty( "field", absoluteFieldPath );
		if ( order != null ) {
			innerObject.add( "order", order );
		}
		innerObject.addProperty( "size", size );
		innerObject.addProperty( "min_doc_count", minDocCount );

		JsonObject subOuterObject = new JsonObject();
		context.add( buildingContextKey( INNER_EXTRACTOR ),
				aggregation.request( context, AggregationKey.of( "agg" ), subOuterObject ) );

		if ( !subOuterObject.isEmpty() ) {
			outerObject.add( "aggs", subOuterObject );
		}
	}

	@Override
	protected Extractor<Map<K, V>> extractor(AggregationKey<?> key, AggregationRequestBuildingContextContext context) {
		Extractor<V> innerExtractor = context.get( buildingContextKey( INNER_EXTRACTOR ) );
		return new TermsBucketExtractor( key, nestedPathHierarchy, filter, innerExtractor );
	}

	public static class Factory<F>
			extends AbstractElasticsearchCodecAwareSearchQueryElementFactory<TermsAggregationBuilder.TypeSelector, F> {
		public Factory(ElasticsearchFieldCodec<F> codec) {
			super( codec );
		}

		@Override
		public TermsAggregationBuilder.TypeSelector create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field) {
			return new TypeSelector<>( codec, scope, field );
		}
	}

	private static class TypeSelector<F> implements TermsAggregationBuilder.TypeSelector {
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
		public <T> Builder<F, T, ?, Long> type(Class<T> expectedType, ValueModel valueModel) {
			if ( ValueModel.RAW.equals( valueModel ) ) {
				return new CountBuilder<>(
						scope, field,
						(key, string) -> string != null && !string.isJsonNull() ? string : key,
						// unchecked cast to make eclipse-compiler happy
						// we know that Elasticsearch projection converters work with the String
						( (ProjectionConverter<JsonElement, ?>) field.type().rawProjectionConverter() )
								.withConvertedType( expectedType, field )
				);
			}
			else {
				return new CountBuilder<>( scope, field, codec::decodeAggregationKey,
						field.type().projectionConverter( valueModel ).withConvertedType( expectedType, field ) );
			}
		}
	}

	protected class TermsBucketExtractor extends AbstractBucketExtractor<K, V> {
		private final Extractor<V> innerExtractor;

		protected TermsBucketExtractor(AggregationKey<?> key, List<String> nestedPathHierarchy,
				ElasticsearchSearchPredicate filter, Extractor<V> innerExtractor) {
			super( key, nestedPathHierarchy, filter );
			this.innerExtractor = innerExtractor;
		}

		@Override
		protected Map<K, V> doExtract(AggregationExtractContext context, JsonElement buckets) {
			JsonArray bucketArray = buckets.getAsJsonArray();
			Map<K, V> result = CollectionHelper.newLinkedHashMap( bucketArray.size() );
			FromDocumentValueConvertContext convertContext = context.fromDocumentValueConvertContext();
			for ( JsonElement bucketElement : bucketArray ) {
				JsonObject bucket = bucketElement.getAsJsonObject();
				JsonElement keyJson = bucket.get( "key" );
				JsonElement keyAsStringJson = bucket.get( "key_as_string" );
				K key = fromFieldValueConverter.fromDocumentValue(
						decodeFunction.apply( keyJson, keyAsStringJson ),
						convertContext
				);
				result.put( key, innerExtractor.extract( bucket, context ) );
			}
			return result;
		}
	}

	private static class CountBuilder<F, K, T> extends Builder<F, K, T, Long> {

		protected CountBuilder(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field,
				BiFunction<JsonElement, JsonElement, T> decodeFunction,
				ProjectionConverter<T, ? extends K> fromFieldValueConverter) {
			super( scope, field, decodeFunction, fromFieldValueConverter,
					ElasticsearchSearchAggregation.from( scope,
							ElasticsearchCountDocumentAggregation.factory( !field.nestedPathHierarchy().isEmpty() )
									.create( scope, null ).type().build() ) );
		}
	}

	private static class Builder<F, K, T, V> extends AbstractBuilder<K, V>
			implements TermsAggregationBuilder<K, V> {

		private final BiFunction<JsonElement, JsonElement, T> decodeFunction;
		private final ProjectionConverter<T, ? extends K> fromFieldValueConverter;
		private final ElasticsearchSearchAggregation<V> aggregation;

		private JsonObject order;
		private int minDocCount;
		private int size;

		private Builder(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field,
				BiFunction<JsonElement, JsonElement, T> decodeFunction,
				ProjectionConverter<T, ? extends K> fromFieldValueConverter,
				ElasticsearchSearchAggregation<V> aggregation) {
			this( scope, field, decodeFunction, fromFieldValueConverter, aggregation, null, 1, 100 );
		}

		public Builder(ElasticsearchSearchIndexScope<?> scope, ElasticsearchSearchIndexValueFieldContext<?> field,
				BiFunction<JsonElement, JsonElement, T> decodeFunction,
				ProjectionConverter<T, ? extends K> fromFieldValueConverter,
				ElasticsearchSearchAggregation<V> aggregation,
				JsonObject order, int minDocCount, int size) {
			super( scope, field );
			this.order = order;
			this.decodeFunction = decodeFunction;
			this.fromFieldValueConverter = fromFieldValueConverter;
			this.aggregation = aggregation;
			this.minDocCount = minDocCount;
			this.size = size;
		}

		@Override
		public void orderByCountDescending() {
			order( "_count", "desc" );
		}

		@Override
		public void orderByCountAscending() {
			order( "_count", "asc" );
		}

		@Override
		public void orderByTermAscending() {
			order( scope.searchSyntax().getTermAggregationOrderByTermToken(), "asc" );
		}

		@Override
		public void orderByTermDescending() {
			order( scope.searchSyntax().getTermAggregationOrderByTermToken(), "desc" );
		}

		@Override
		public void minDocumentCount(int minDocumentCount) {
			this.minDocCount = minDocumentCount;
		}

		@Override
		public void maxTermCount(int maxTermCount) {
			this.size = maxTermCount;
		}

		@Override
		public <R> TermsAggregationBuilder<K, R> withValue(SearchAggregation<R> aggregation) {
			return new Builder<>( scope, field, decodeFunction, fromFieldValueConverter,
					ElasticsearchSearchAggregation.from( scope, aggregation ), order, minDocCount, size );
		}

		@Override
		public ElasticsearchTermsAggregation<F, K, T, V> build() {
			return new ElasticsearchTermsAggregation<>( this );
		}

		protected final void order(String key, String order) {
			JsonObject orderObject = new JsonObject();
			orderObject.addProperty( key, order );
			this.order = orderObject;
		}
	}
}
