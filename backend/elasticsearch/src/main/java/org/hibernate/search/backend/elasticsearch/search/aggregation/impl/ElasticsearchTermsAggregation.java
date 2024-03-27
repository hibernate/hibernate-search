/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.util.common.impl.CollectionHelper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @param <F> The type of field values.
 * @param <K> The type of keys in the returned map. It can be {@code F}
 * or a different type if value converters are used.
 */
public class ElasticsearchTermsAggregation<F, K>
		extends AbstractElasticsearchBucketAggregation<K, Long> {

	private final String absoluteFieldPath;

	private final ProjectionConverter<F, ? extends K> fromFieldValueConverter;
	private final ElasticsearchFieldCodec<F> codec;

	private final JsonObject order;
	private final int size;
	private final int minDocCount;

	private ElasticsearchTermsAggregation(Builder<F, K> builder) {
		super( builder );
		this.absoluteFieldPath = builder.field.absolutePath();
		this.fromFieldValueConverter = builder.fromFieldValueConverter;
		this.codec = builder.codec;
		this.order = builder.order;
		this.size = builder.size;
		this.minDocCount = builder.minDocCount;
	}

	@Override
	protected void doRequest(JsonObject outerObject, JsonObject innerObject) {
		outerObject.add( "terms", innerObject );
		innerObject.addProperty( "field", absoluteFieldPath );
		if ( order != null ) {
			innerObject.add( "order", order );
		}
		innerObject.addProperty( "size", size );
		innerObject.addProperty( "min_doc_count", minDocCount );
	}

	@Override
	protected Map<K, Long> doExtract(AggregationExtractContext context, JsonElement buckets) {
		JsonArray bucketArray = buckets.getAsJsonArray();
		Map<K, Long> result = CollectionHelper.newLinkedHashMap( bucketArray.size() );
		FromDocumentValueConvertContext convertContext = context.fromDocumentValueConvertContext();
		for ( JsonElement bucketElement : bucketArray ) {
			JsonObject bucket = bucketElement.getAsJsonObject();
			JsonElement keyJson = bucket.get( "key" );
			JsonElement keyAsStringJson = bucket.get( "key_as_string" );
			K key = fromFieldValueConverter.fromDocumentValue(
					codec.decodeAggregationKey( keyJson, keyAsStringJson ),
					convertContext
			);
			long documentCount = getBucketDocCount( bucket );
			result.put( key, documentCount );
		}
		return result;
	}

	public static class Factory<F>
			extends AbstractElasticsearchCodecAwareSearchQueryElementFactory<TermsAggregationBuilder.TypeSelector, F> {
		public Factory(ElasticsearchFieldCodec<F> codec) {
			super( codec );
		}

		@Override
		public TypeSelector<?> create(ElasticsearchSearchIndexScope<?> scope,
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

		@Override
		public <T> Builder<F, T> type(Class<T> expectedType, ValueConvert convert) {
			return new Builder<>( codec, scope, field,
					field.type().projectionConverter( convert ).withConvertedType( expectedType, field ) );
		}
	}

	private static class Builder<F, K> extends AbstractBuilder<K, Long>
			implements TermsAggregationBuilder<K> {

		private final ElasticsearchFieldCodec<F> codec;
		private final ProjectionConverter<F, ? extends K> fromFieldValueConverter;

		private JsonObject order;
		private int minDocCount = 1;
		private int size = 100;

		private Builder(ElasticsearchFieldCodec<F> codec, ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field,
				ProjectionConverter<F, ? extends K> fromFieldValueConverter) {
			super( scope, field );
			this.codec = codec;
			this.fromFieldValueConverter = fromFieldValueConverter;
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
		public ElasticsearchTermsAggregation<F, K> build() {
			return new ElasticsearchTermsAggregation<>( this );
		}

		protected final void order(String key, String order) {
			JsonObject orderObject = new JsonObject();
			orderObject.addProperty( key, order );
			this.order = orderObject;
		}
	}
}
