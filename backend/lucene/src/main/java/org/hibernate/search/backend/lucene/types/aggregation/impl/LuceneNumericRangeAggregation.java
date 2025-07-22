/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.RangeCollector;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.RangeCollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.RangeResults;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.JoiningLongMultiValuesSource;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationExtractContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationRequestContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.LuceneSearchAggregation;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.util.common.data.Range;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.CollectorManager;

/**
 * @param <F> The type of field values.
 * @param <E> The type of encoded field values.
 * @param <K> The type of keys in the returned map. It can be {@code F}
 * @param <V> The type of aggregated values.
 * or a different type if value converters are used.
 */
public class LuceneNumericRangeAggregation<F, E extends Number, K, V>
		extends AbstractLuceneBucketAggregation<Range<K>, V> {

	private final LuceneSearchAggregation<V> aggregation;
	private final AbstractLuceneNumericFieldCodec<?, E> codec;

	private final List<Range<K>> rangesInOrder;
	private final List<Range<E>> encodedRangesInOrder;

	private CollectorKey<RangeCollector, RangeResults> collectorKey;

	private LuceneNumericRangeAggregation(Builder<F, E, K, V> builder) {
		super( builder );
		this.aggregation = builder.aggregation;
		this.codec = builder.codec;
		this.rangesInOrder = builder.rangesInOrder;
		this.encodedRangesInOrder = builder.encodedRangesInOrder;
	}

	@Override
	public Extractor<Map<Range<K>, V>> request(AggregationRequestContext context) {
		NestedDocsProvider nestedDocsProvider = createNestedDocsProvider( context );
		JoiningLongMultiValuesSource source = JoiningLongMultiValuesSource.fromLongField(
				absoluteFieldPath, nestedDocsProvider
		);

		LocalAggregationRequestContext localAggregationContext = new LocalAggregationRequestContext( context );
		Extractor<V> extractor = aggregation.request( localAggregationContext );

		var rangeFactory = RangeCollectorFactory.instance( source,
				codec.getDomain().createEffectiveRanges( encodedRangesInOrder ),
				localAggregationContext.localCollectorFactories() );

		collectorKey = rangeFactory.getCollectorKey();
		context.requireCollector( rangeFactory );

		return new LuceneNumericRangeAggregationExtractor( extractor );
	}

	public static class Factory<F>
			extends
			AbstractLuceneCodecAwareSearchQueryElementFactory<RangeAggregationBuilder.TypeSelector,
					F,
					AbstractLuceneNumericFieldCodec<F, ?>> {
		public Factory(AbstractLuceneNumericFieldCodec<F, ?> codec) {
			super( codec );
		}

		@Override
		public TypeSelector<?, ?> create(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			return new TypeSelector<>( codec, scope, field );
		}
	}

	private class LuceneNumericRangeAggregationExtractor implements Extractor<Map<Range<K>, V>> {
		private final Extractor<V> extractor;

		public LuceneNumericRangeAggregationExtractor(Extractor<V> extractor) {
			this.extractor = extractor;
		}

		@Override
		public Map<Range<K>, V> extract(AggregationExtractContext context) throws IOException {
			RangeResults rangeResults = context.getCollectorResults( collectorKey );

			LocalAggregationExtractContext localContext = new LocalAggregationExtractContext( context );

			Map<Range<K>, V> result = new LinkedHashMap<>();
			for ( int i = 0; i < rangesInOrder.size(); i++ ) {
				localContext.setResults( prepareResults( i, rangeResults ) );
				result.put( rangesInOrder.get( i ), extractor.extract( localContext ) );
			}

			return result;
		}

		private Map<CollectorKey<?, ?>, Object> prepareResults(int index, RangeResults rangeResults) throws IOException {
			Map<CollectorKey<?, ?>, Object> result = new HashMap<>();
			List<Collector>[][] collectors = rangeResults.buckets();
			CollectorKey<?, ?>[] collectorKeys = rangeResults.collectorKeys();
			CollectorManager<Collector, ?>[] managers = rangeResults.collectorManagers();
			for ( int i = 0; i < collectorKeys.length; i++ ) {
				result.put( collectorKeys[i], managers[i].reduce( collectors[i][index] ) );
			}
			return result;
		}
	}

	public static class TypeSelector<F, E extends Number> implements RangeAggregationBuilder.TypeSelector {
		private final AbstractLuceneNumericFieldCodec<F, E> codec;
		private final LuceneSearchIndexScope<?> scope;
		private final LuceneSearchIndexValueFieldContext<F> field;

		private TypeSelector(AbstractLuceneNumericFieldCodec<F, E> codec,
				LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			this.codec = codec;
			this.scope = scope;
			this.field = field;
		}

		@Override
		public <K> Builder<F, ?, K, Long> type(Class<K> expectedType, ValueModel valueModel) {
			return new CountBuilder<>(
					codec, field.encodingContext().encoder( scope, field, codec, expectedType, valueModel ),
					scope, field
			);
		}
	}

	public static class Builder<F, E extends Number, K, V>
			extends AbstractLuceneBucketAggregation.AbstractBuilder<Range<K>, V>
			implements RangeAggregationBuilder<K, V> {

		private final AbstractLuceneNumericFieldCodec<F, E> codec;
		private final Function<K, E> convertAndEncode;

		private final LuceneSearchAggregation<V> aggregation;
		private final List<Range<K>> rangesInOrder;
		private final List<Range<E>> encodedRangesInOrder;

		protected Builder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<?> field,
				AbstractLuceneNumericFieldCodec<F, E> codec, Function<K, E> convertAndEncode,
				LuceneSearchAggregation<V> aggregation, List<Range<K>> rangesInOrder, List<Range<E>> encodedRangesInOrder) {
			super( scope, field );
			this.codec = codec;
			this.convertAndEncode = convertAndEncode;
			this.aggregation = aggregation;
			this.rangesInOrder = rangesInOrder;
			this.encodedRangesInOrder = encodedRangesInOrder;
		}

		@Override
		public void range(Range<? extends K> range) {
			rangesInOrder.add( range.map( Function.identity() ) );
			encodedRangesInOrder.add( range.map( convertAndEncode ) );
		}

		@Override
		public <T> RangeAggregationBuilder<K, T> withValue(SearchAggregation<T> aggregation) {
			return new Builder<>( scope, field, codec, convertAndEncode, LuceneSearchAggregation.from( scope, aggregation ),
					new ArrayList<>( rangesInOrder ), new ArrayList<>( encodedRangesInOrder ) );
		}

		@Override
		public LuceneNumericRangeAggregation<F, E, K, V> build() {
			return new LuceneNumericRangeAggregation<>( this );
		}
	}

	public static class CountBuilder<F, E extends Number, K> extends Builder<F, E, K, Long> {

		protected CountBuilder(AbstractLuceneNumericFieldCodec<F, E> codec, Function<K, E> convertAndEncode,
				LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			super( scope, field, codec, convertAndEncode,
					LuceneSearchAggregation.from( scope,
							LuceneCountDocumentAggregation.factory().create( scope, field ).builder().build() ),
					new ArrayList<>(), new ArrayList<>() );
		}
	}

}
