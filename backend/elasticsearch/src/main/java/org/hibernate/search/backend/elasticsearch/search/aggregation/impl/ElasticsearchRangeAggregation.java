/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.AbstractElasticsearchCodecAwareSearchValueFieldQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchValueFieldContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.data.RangeBoundInclusion;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @param <F> The type of field values.
 * @param <K> The type of keys in the returned map. It can be {@code F}
 * or a different type if value converters are used.
 */
public class ElasticsearchRangeAggregation<F, K>
		extends AbstractElasticsearchBucketAggregation<Range<K>, Long> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String absoluteFieldPath;

	private final List<Range<K>> rangesInOrder;
	private final JsonArray rangesJson;

	private ElasticsearchRangeAggregation(Builder<F, K> builder) {
		super( builder );
		this.absoluteFieldPath = builder.field.absolutePath();
		this.rangesInOrder = builder.rangesInOrder;
		this.rangesJson = builder.rangesJson;
	}

	@Override
	protected void doRequest(AggregationRequestContext context, JsonObject outerObject, JsonObject innerObject) {
		outerObject.add( "range", innerObject );
		innerObject.addProperty( "field", absoluteFieldPath );
		innerObject.addProperty( "keyed", true );
		innerObject.add( "ranges", rangesJson );
	}

	@Override
	protected Map<Range<K>, Long> doExtract(AggregationExtractContext context, JsonObject outerObject,
			JsonElement buckets) {
		JsonObject bucketMap = buckets.getAsJsonObject();
		Map<Range<K>, Long> result = CollectionHelper.newLinkedHashMap( rangesJson.size() );
		for ( int i = 0; i < rangesJson.size(); i++ ) {
			JsonObject bucket = bucketMap.get( String.valueOf( i ) ).getAsJsonObject();
			Range<K> range = rangesInOrder.get( i );
			long documentCount = getBucketDocCount( bucket );
			result.put( range, documentCount );
		}
		return result;
	}

	public static class Factory<F>
			extends AbstractElasticsearchCodecAwareSearchValueFieldQueryElementFactory<TypeSelector<?>, F> {
		public Factory(ElasticsearchFieldCodec<F> codec) {
			super( codec );
		}

		@Override
		public TypeSelector<?> create(ElasticsearchSearchContext searchContext,
				ElasticsearchSearchValueFieldContext<F> field) {
			return new TypeSelector<>( codec, searchContext, field );
		}
	}

	public static class TypeSelector<F> {
		private final ElasticsearchFieldCodec<F> codec;
		private final ElasticsearchSearchContext searchContext;
		private final ElasticsearchSearchValueFieldContext<F> field;

		private TypeSelector(ElasticsearchFieldCodec<F> codec,
				ElasticsearchSearchContext searchContext, ElasticsearchSearchValueFieldContext<F> field) {
			this.codec = codec;
			this.searchContext = searchContext;
			this.field = field;
		}

		public <T> Builder<F, T> type(Class<T> expectedType, ValueConvert convert) {
			return new Builder<>( codec, searchContext, field,
					field.type().dslConverter( convert ).withInputType( expectedType, field ) );
		}
	}

	public static class Builder<F, K> extends AbstractBuilder<Range<K>, Long>
			implements RangeAggregationBuilder<K> {

		private final ElasticsearchFieldCodec<F> codec;
		private final DslConverter<? super K, F> toFieldValueConverter;

		private final List<Range<K>> rangesInOrder = new ArrayList<>();
		private final JsonArray rangesJson = new JsonArray();

		private Builder(ElasticsearchFieldCodec<F> codec, ElasticsearchSearchContext searchContext,
				ElasticsearchSearchValueFieldContext<F> field, DslConverter<? super K, F> toFieldValueConverter) {
			super( searchContext, field );
			this.codec = codec;
			this.toFieldValueConverter = toFieldValueConverter;
		}

		@Override
		public void range(Range<? extends K> range) {
			JsonObject rangeJson = new JsonObject();
			Optional<? extends K> lowerBoundValue = range.lowerBoundValue();
			if ( lowerBoundValue.isPresent() ) {
				if ( !RangeBoundInclusion.INCLUDED.equals( range.lowerBoundInclusion() ) ) {
					throw log.elasticsearchRangeAggregationRequiresCanonicalFormForRanges( range );
				}
				rangeJson.add( "from", convertToFieldValue( lowerBoundValue.get() ) );
			}
			Optional<? extends K> upperBoundValue = range.upperBoundValue();
			if ( upperBoundValue.isPresent() ) {
				if ( !RangeBoundInclusion.EXCLUDED.equals( range.upperBoundInclusion() ) ) {
					throw log.elasticsearchRangeAggregationRequiresCanonicalFormForRanges( range );
				}
				rangeJson.add( "to", convertToFieldValue( upperBoundValue.get() ) );
			}
			// We need to request a keyed response,
			// because ranges are not always returned in the order they are submitted
			rangeJson.addProperty( "key", String.valueOf( rangesJson.size() ) );
			rangesInOrder.add( range.map( Function.identity() ) );
			rangesJson.add( rangeJson );
		}

		@Override
		public ElasticsearchRangeAggregation<F, K> build() {
			return new ElasticsearchRangeAggregation<>( this );
		}

		private JsonElement convertToFieldValue(K value) {
			try {
				F converted = toFieldValueConverter.convert( value, searchContext.toDocumentFieldValueConvertContext() );
				return codec.encode( converted );
			}
			catch (RuntimeException e) {
				throw log.cannotConvertDslParameter( e.getMessage(), e, field.eventContext() );
			}
		}
	}
}
