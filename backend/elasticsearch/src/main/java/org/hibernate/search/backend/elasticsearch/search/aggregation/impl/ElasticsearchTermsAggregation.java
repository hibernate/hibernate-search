/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
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

	private final ProjectionConverter<? super F, ? extends K> fromFieldValueConverter;
	private final ElasticsearchFieldCodec<F> codec;

	private final JsonObject order;
	private final int size;
	private final int minDocCount;

	private ElasticsearchTermsAggregation(Builder<F, K> builder) {
		super( builder );
		this.absoluteFieldPath = builder.absoluteFieldPath;
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

	public static class Builder<F, K> extends AbstractBuilder<K, Long>
			implements TermsAggregationBuilder<K> {

		private final ProjectionConverter<? super F, ? extends K> fromFieldValueConverter;
		private final ElasticsearchFieldCodec<F> codec;

		private JsonObject order;
		private int minDocCount = 1;
		private int size = 100;

		public Builder(ElasticsearchSearchContext searchContext, String absoluteFieldPath,
				List<String> nestedPathHierarchy,
				ProjectionConverter<? super F, ? extends K> fromFieldValueConverter,
				ElasticsearchFieldCodec<F> codec) {
			super( searchContext, absoluteFieldPath, nestedPathHierarchy );
			this.fromFieldValueConverter = fromFieldValueConverter;
			this.codec = codec;
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
			order( searchContext.getSearchSyntax().getTermAggregationOrderByTermToken(), "asc" );
		}

		@Override
		public void orderByTermDescending() {
			order( searchContext.getSearchSyntax().getTermAggregationOrderByTermToken(), "desc" );
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
