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
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
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
		this.absoluteFieldPath = builder.absoluteFieldPath;
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
			long documentCount = bucket.get( "doc_count" ).getAsLong();
			result.put( range, documentCount );
		}
		return result;
	}

	public static class Builder<F, K> extends AbstractBuilder<Range<K>, Long>
			implements RangeAggregationBuilder<K> {

		private final String absoluteFieldPath;
		private final List<String> nestedPathHierarchy;

		private final DslConverter<?, ? extends F> toFieldValueConverter;
		private final ElasticsearchFieldCodec<F> codec;

		private final List<Range<K>> rangesInOrder = new ArrayList<>();
		private final JsonArray rangesJson = new JsonArray();

		public Builder(ElasticsearchSearchContext searchContext, String absoluteFieldPath,
				List<String> nestedPathHierarchy,
				DslConverter<?, ? extends F> toFieldValueConverter,
				ElasticsearchFieldCodec<F> codec) {
			super( searchContext );
			this.absoluteFieldPath = absoluteFieldPath;
			this.nestedPathHierarchy = nestedPathHierarchy;
			this.toFieldValueConverter = toFieldValueConverter;
			this.codec = codec;
		}

		@Override
		public void range(Range<? extends K> range) {
			JsonObject rangeJson = new JsonObject();
			Optional<? extends K> lowerBoundValue = range.getLowerBoundValue();
			if ( lowerBoundValue.isPresent() ) {
				if ( !RangeBoundInclusion.INCLUDED.equals( range.getLowerBoundInclusion() ) ) {
					throw log.elasticsearchRangeAggregationRequiresCanonicalFormForRanges( range );
				}
				rangeJson.add( "from", convertToFieldValue( lowerBoundValue.get() ) );
			}
			Optional<? extends K> upperBoundValue = range.getUpperBoundValue();
			if ( upperBoundValue.isPresent() ) {
				if ( !RangeBoundInclusion.EXCLUDED.equals( range.getUpperBoundInclusion() ) ) {
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

		@Override
		protected List<String> getNestedPathHierarchy() {
			return nestedPathHierarchy;
		}

		private JsonElement convertToFieldValue(K value) {
			try {
				F converted = toFieldValueConverter.convertUnknown( value, searchContext.getToDocumentFieldValueConvertContext() );
				return codec.encode( converted );
			}
			catch (RuntimeException e) {
				throw log.cannotConvertDslParameter(
						e.getMessage(), e, EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
				);
			}
		}
	}
}
