/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.search.impl.AbstractElasticsearchCodecAwareSearchFieldQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
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
	protected void doRequest(AggregationRequestContext context, JsonObject outerObject, JsonObject innerObject) {
		outerObject.add( "terms", innerObject );
		innerObject.addProperty( "field", absoluteFieldPath );
		if ( order != null ) {
			innerObject.add( "order", order );
		}
		innerObject.addProperty( "size", size );
		innerObject.addProperty( "min_doc_count", minDocCount );
	}

	@Override
	protected Map<K, Long> doExtract(AggregationExtractContext context, JsonObject outerObject, JsonElement buckets) {
		JsonArray bucketArray = buckets.getAsJsonArray();
		Map<K, Long> result = CollectionHelper.newLinkedHashMap( bucketArray.size() );
		FromDocumentFieldValueConvertContext convertContext = context.getConvertContext();
		for ( JsonElement bucketElement : bucketArray ) {
			JsonObject bucket = bucketElement.getAsJsonObject();
			JsonElement keyJson = bucket.get( "key" );
			JsonElement keyAsStringJson = bucket.get( "key_as_string" );
			K key = fromFieldValueConverter.convert(
					codec.decodeAggregationKey( keyJson, keyAsStringJson ),
					convertContext
			);
			long documentCount = getBucketDocCount( bucket );
			result.put( key, documentCount );
		}
		return result;
	}

	public static class Factory<F>
			extends AbstractElasticsearchCodecAwareSearchFieldQueryElementFactory<TypeSelector<?>, F> {
		public Factory(ElasticsearchFieldCodec<F> codec) {
			super( codec );
		}

		@Override
		public TypeSelector<?> create(ElasticsearchSearchContext searchContext,
				ElasticsearchSearchFieldContext<F> field) {
			return new TypeSelector<>( codec, searchContext, field );
		}
	}

	public static class TypeSelector<F> {
		private final ElasticsearchFieldCodec<F> codec;
		private final ElasticsearchSearchContext searchContext;
		private final ElasticsearchSearchFieldContext<F> field;

		private TypeSelector(ElasticsearchFieldCodec<F> codec,
				ElasticsearchSearchContext searchContext, ElasticsearchSearchFieldContext<F> field) {
			this.codec = codec;
			this.searchContext = searchContext;
			this.field = field;
		}

		public <T> Builder<F, T> type(Class<T> expectedType, ValueConvert convert) {
			return new Builder<>( codec, searchContext, field,
					field.type().projectionConverter( convert ).withConvertedType( expectedType, field ) );
		}
	}

	public static class Builder<F, K> extends AbstractBuilder<K, Long>
			implements TermsAggregationBuilder<K> {

		private final ElasticsearchFieldCodec<F> codec;
		private final ProjectionConverter<F, ? extends K> fromFieldValueConverter;

		private JsonObject order;
		private int minDocCount = 1;
		private int size = 100;

		private Builder(ElasticsearchFieldCodec<F> codec, ElasticsearchSearchContext searchContext,
				ElasticsearchSearchFieldContext<F> field,
				ProjectionConverter<F, ? extends K> fromFieldValueConverter) {
			super( searchContext, field );
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
			order( searchContext.searchSyntax().getTermAggregationOrderByTermToken(), "asc" );
		}

		@Override
		public void orderByTermDescending() {
			order( searchContext.searchSyntax().getTermAggregationOrderByTermToken(), "desc" );
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
