/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.FacetsCollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationExtractContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationRequestContext;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;
import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.util.common.data.Range;

import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;

/**
 * @param <F> The type of field values.
 * @param <E> The type of encoded field values.
 * @param <K> The type of keys in the returned map. It can be {@code F}
 * or a different type if value converters are used.
 */
public class LuceneNumericRangeAggregation<F, E extends Number, K>
		extends AbstractLuceneBucketAggregation<Range<K>, Long> {

	private final AbstractLuceneNumericFieldCodec<?, E> codec;

	private final List<Range<K>> rangesInOrder;
	private final List<Range<E>> encodedRangesInOrder;

	private LuceneNumericRangeAggregation(Builder<F, E, K> builder) {
		super( builder );
		this.codec = builder.codec;
		this.rangesInOrder = builder.rangesInOrder;
		this.encodedRangesInOrder = builder.encodedRangesInOrder;
	}

	@Override
	public Extractor<Map<Range<K>, Long>> request(AggregationRequestContext context) {
		context.requireCollector( FacetsCollectorFactory.INSTANCE );

		return new LuceneNumericRangeAggregationExtractor();
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

	private class LuceneNumericRangeAggregationExtractor implements Extractor<Map<Range<K>, Long>> {

		@Override
		public Map<Range<K>, Long> extract(AggregationExtractContext context) throws IOException {
			LuceneNumericDomain<E> numericDomain = codec.getDomain();

			FacetsCollector facetsCollector = context.getFacets( FacetsCollectorFactory.KEY );

			NestedDocsProvider nestedDocsProvider = createNestedDocsProvider( context );

			Facets facetsCount = numericDomain.createRangeFacetCounts(
					absoluteFieldPath, facetsCollector, encodedRangesInOrder,
					nestedDocsProvider
			);

			FacetResult facetResult = facetsCount.getTopChildren( rangesInOrder.size(), absoluteFieldPath );

			Map<Range<K>, Long> result = new LinkedHashMap<>();
			for ( int i = 0; i < rangesInOrder.size(); i++ ) {
				result.put( rangesInOrder.get( i ), (long) (Integer) facetResult.labelValues[i].value );
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
		public <K> Builder<F, ?, K> type(Class<K> expectedType, ValueModel valueModel) {
			return new Builder<>( codec,
					field.encodingContext().encoder( scope, field, codec, expectedType, valueModel ),
					scope, field );
		}
	}

	public static class Builder<F, E extends Number, K>
			extends AbstractLuceneBucketAggregation.AbstractBuilder<Range<K>, Long>
			implements RangeAggregationBuilder<K> {

		private final AbstractLuceneNumericFieldCodec<F, E> codec;
		private final Function<K, E> convertAndEncode;

		private final List<Range<K>> rangesInOrder = new ArrayList<>();
		private final List<Range<E>> encodedRangesInOrder = new ArrayList<>();

		public Builder(AbstractLuceneNumericFieldCodec<F, E> codec, Function<K, E> convertAndEncode,
				LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<?> field) {
			super( scope, field );
			this.codec = codec;
			this.convertAndEncode = convertAndEncode;
		}

		@Override
		public void range(Range<? extends K> range) {
			rangesInOrder.add( range.map( Function.identity() ) );
			encodedRangesInOrder.add( range.map( convertAndEncode ) );
		}

		@Override
		public LuceneNumericRangeAggregation<F, E, K> build() {
			return new LuceneNumericRangeAggregation<>( this );
		}
	}
}
