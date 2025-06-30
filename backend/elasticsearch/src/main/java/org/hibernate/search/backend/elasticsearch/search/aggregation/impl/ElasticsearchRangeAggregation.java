/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.logging.impl.QueryLog;
import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicate;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.data.RangeBoundInclusion;
import org.hibernate.search.util.common.impl.CollectionHelper;

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
	protected void doRequest(JsonObject outerObject, JsonObject innerObject) {
		outerObject.add( "range", innerObject );
		innerObject.addProperty( "field", absoluteFieldPath );
		innerObject.addProperty( "keyed", true );
		innerObject.add( "ranges", rangesJson );
	}

	@Override
	protected Extractor<Map<Range<K>, Long>> extractor(AggregationRequestContext context) {
		return new RangeBucketExtractor( nestedPathHierarchy, filter, rangesInOrder );
	}

	public static class Factory<F>
			extends AbstractElasticsearchCodecAwareSearchQueryElementFactory<RangeAggregationBuilder.TypeSelector, F> {
		public Factory(ElasticsearchFieldCodec<F> codec) {
			super( codec );
		}

		@Override
		public RangeAggregationBuilder.TypeSelector create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field) {
			return new TypeSelector<>( scope, field );
		}
	}

	private static class TypeSelector<F> implements RangeAggregationBuilder.TypeSelector {
		private final ElasticsearchSearchIndexScope<?> scope;
		private final ElasticsearchSearchIndexValueFieldContext<F> field;

		private TypeSelector(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field) {
			this.scope = scope;
			this.field = field;
		}

		@Override
		public <T> Builder<F, T> type(Class<T> expectedType, ValueModel valueModel) {
			return new Builder<>( scope, field, field.encodingContext().encoder( scope, field, expectedType, valueModel ) );
		}
	}

	protected class RangeBucketExtractor extends AbstractBucketExtractor<Range<K>, Long> {
		private final List<Range<K>> rangesInOrder;

		protected RangeBucketExtractor(List<String> nestedPathHierarchy, ElasticsearchSearchPredicate filter,
				List<Range<K>> rangesInOrder) {
			super( nestedPathHierarchy, filter );
			this.rangesInOrder = rangesInOrder;
		}


		@Override
		protected Map<Range<K>, Long> doExtract(AggregationExtractContext context, JsonElement buckets) {
			JsonObject bucketMap = buckets.getAsJsonObject();
			Map<Range<K>, Long> result = CollectionHelper.newLinkedHashMap( rangesInOrder.size() );
			for ( int i = 0; i < rangesInOrder.size(); i++ ) {
				JsonObject bucket = bucketMap.get( String.valueOf( i ) ).getAsJsonObject();
				Range<K> range = rangesInOrder.get( i );
				long documentCount = getBucketDocCount( bucket );
				result.put( range, documentCount );
			}
			return result;
		}
	}

	private static class Builder<F, K> extends AbstractBuilder<Range<K>, Long>
			implements RangeAggregationBuilder<K, Long> {

		private final Function<? super K, JsonElement> encoder;

		private final List<Range<K>> rangesInOrder = new ArrayList<>();
		private final JsonArray rangesJson = new JsonArray();

		private Builder(ElasticsearchSearchIndexScope<?> scope, ElasticsearchSearchIndexValueFieldContext<F> field,
				Function<? super K, JsonElement> encoder) {
			super( scope, field );
			this.encoder = encoder;
		}

		@Override
		public void range(Range<? extends K> range) {
			JsonObject rangeJson = new JsonObject();
			Optional<? extends K> lowerBoundValue = range.lowerBoundValue();
			if ( lowerBoundValue.isPresent() ) {
				if ( !RangeBoundInclusion.INCLUDED.equals( range.lowerBoundInclusion() ) ) {
					throw QueryLog.INSTANCE.elasticsearchRangeAggregationRequiresCanonicalFormForRanges( range );
				}
				rangeJson.add( "from", encoder.apply( lowerBoundValue.get() ) );
			}
			Optional<? extends K> upperBoundValue = range.upperBoundValue();
			if ( upperBoundValue.isPresent() ) {
				if ( !RangeBoundInclusion.EXCLUDED.equals( range.upperBoundInclusion() ) ) {
					throw QueryLog.INSTANCE.elasticsearchRangeAggregationRequiresCanonicalFormForRanges( range );
				}
				rangeJson.add( "to", encoder.apply( upperBoundValue.get() ) );
			}
			// We need to request a keyed response,
			// because ranges are not always returned in the order they are submitted
			rangeJson.addProperty( "key", String.valueOf( rangesJson.size() ) );
			rangesInOrder.add( range.map( Function.identity() ) );
			rangesJson.add( rangeJson );
		}

		@Override
		public <T> RangeAggregationBuilder<K, T> withValue(SearchAggregation<T> aggregation) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ElasticsearchRangeAggregation<F, K> build() {
			return new ElasticsearchRangeAggregation<>( this );
		}

	}
}
