/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import static org.hibernate.search.backend.elasticsearch.search.aggregation.impl.AggregationRequestBuildingContextContext.buildingContextKey;

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
import org.hibernate.search.engine.search.aggregation.AggregationKey;
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
 * @param <V> The type of aggregated values.
 * or a different type if value converters are used.
 */
public class ElasticsearchRangeAggregation<F, K, V>
		extends AbstractElasticsearchBucketAggregation<Range<K>, V> {

	private final String absoluteFieldPath;

	private final List<Range<K>> rangesInOrder;
	private final JsonArray rangesJson;

	private final ElasticsearchSearchAggregation<V> aggregation;

	private ElasticsearchRangeAggregation(Builder<F, K, V> builder) {
		super( builder );
		this.absoluteFieldPath = builder.field.absolutePath();
		this.rangesInOrder = builder.rangesInOrder;
		this.rangesJson = builder.rangesJson;
		this.aggregation = builder.aggregation;
	}

	@Override
	protected void doRequest(JsonObject outerObject, JsonObject innerObject, AggregationRequestBuildingContextContext context) {
		outerObject.add( "range", innerObject );
		innerObject.addProperty( "field", absoluteFieldPath );
		innerObject.addProperty( "keyed", true );
		innerObject.add( "ranges", rangesJson );

		JsonObject subOuterObject = new JsonObject();
		context.add( buildingContextKey( INNER_EXTRACTOR ),
				aggregation.request( context, AggregationKey.of( "agg" ), subOuterObject ) );

		if ( !subOuterObject.isEmpty() ) {
			REQUEST_AGGREGATIONS_ACCESSOR.set( outerObject, subOuterObject );
		}
	}

	@Override
	protected Extractor<Map<Range<K>, V>> extractor(AggregationKey<?> key, AggregationRequestBuildingContextContext context) {
		Extractor<V> innerExtractor = context.get( buildingContextKey( INNER_EXTRACTOR ) );
		return new RangeBucketExtractor( key, nestedPathHierarchy, filter, rangesInOrder, innerExtractor );
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
		public <T> Builder<F, T, Long> type(Class<T> expectedType, ValueModel valueModel) {
			return new CountBuilder<>( scope, field,
					field.encodingContext().encoder( scope, field, expectedType, valueModel ) );
		}
	}

	protected class RangeBucketExtractor extends AbstractBucketExtractor<Range<K>, V> {
		private final List<Range<K>> rangesInOrder;
		private final Extractor<V> innerExtractor;

		protected RangeBucketExtractor(AggregationKey<?> key, List<String> nestedPathHierarchy,
				ElasticsearchSearchPredicate filter, List<Range<K>> rangesInOrder, Extractor<V> innerExtractor) {
			super( key, nestedPathHierarchy, filter );
			this.rangesInOrder = rangesInOrder;
			this.innerExtractor = innerExtractor;
		}


		@Override
		protected Map<Range<K>, V> doExtract(AggregationExtractContext context, JsonElement buckets) {
			JsonObject bucketMap = buckets.getAsJsonObject();
			Map<Range<K>, V> result = CollectionHelper.newLinkedHashMap( rangesInOrder.size() );
			for ( int i = 0; i < rangesInOrder.size(); i++ ) {
				JsonObject bucket = bucketMap.get( String.valueOf( i ) ).getAsJsonObject();
				Range<K> range = rangesInOrder.get( i );
				result.put( range, innerExtractor.extract( bucket, context ) );
			}
			return result;
		}
	}

	private static class CountBuilder<F, K> extends Builder<F, K, Long> {

		protected CountBuilder(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<?> field,
				Function<? super K, JsonElement> encoder) {
			super( scope, field, encoder, new ArrayList<>(), new JsonArray(),
					ElasticsearchSearchAggregation.from( scope,
							ElasticsearchCountDocumentAggregation.factory().create( scope, field ).builder().build() ) );
		}
	}

	private static class Builder<F, K, T> extends AbstractBuilder<Range<K>, T>
			implements RangeAggregationBuilder<K, T> {

		private final Function<? super K, JsonElement> encoder;

		private final List<Range<K>> rangesInOrder;
		private final JsonArray rangesJson;
		private final ElasticsearchSearchAggregation<T> aggregation;

		protected Builder(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<?> field,
				Function<? super K, JsonElement> encoder,
				List<Range<K>> rangesInOrder,
				JsonArray rangesJson,
				ElasticsearchSearchAggregation<T> aggregation) {
			super( scope, field );
			this.encoder = encoder;
			this.rangesInOrder = rangesInOrder;
			this.rangesJson = rangesJson;
			this.aggregation = aggregation;
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
		public <N> Builder<F, K, N> withValue(SearchAggregation<N> aggregation) {
			return new Builder<>( scope, field, encoder, rangesInOrder, rangesJson,
					ElasticsearchSearchAggregation.from( scope, aggregation ) );
		}

		@Override
		public ElasticsearchRangeAggregation<F, K, T> build() {
			return new ElasticsearchRangeAggregation<>( this );
		}
	}
}
