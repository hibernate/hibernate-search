/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.FacetsCollectorFactory;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationExtractContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationRequestContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

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

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final AbstractLuceneNumericFieldCodec<F, E> codec;

	private final List<Range<K>> rangesInOrder;
	private final List<Range<E>> encodedRangesInOrder;

	private LuceneNumericRangeAggregation(Builder<F, E, K> builder) {
		super( builder );
		this.codec = builder.codec;
		this.rangesInOrder = builder.rangesInOrder;
		this.encodedRangesInOrder = builder.encodedRangesInOrder;
	}

	@Override
	public void request(AggregationRequestContext context) {
		context.requireCollector( FacetsCollectorFactory.INSTANCE );
		super.request( context );
	}

	@Override
	public Map<Range<K>, Long> extract(AggregationExtractContext context) throws IOException {
		LuceneNumericDomain<E> numericDomain = codec.getDomain();

		FacetsCollector facetsCollector = context.getCollector( FacetsCollectorFactory.KEY );

		String absoluteFieldPath = getAbsoluteFieldPath();

		Facets facetsCount = numericDomain.createRangeFacetCounts(
			absoluteFieldPath, facetsCollector, encodedRangesInOrder,
			getMultiValueMode(), getNestedDocsProvider()
		);

		FacetResult facetResult = facetsCount.getTopChildren( rangesInOrder.size(), absoluteFieldPath );

		Map<Range<K>, Long> result = new LinkedHashMap<>();
		for ( int i = 0; i < rangesInOrder.size(); i++ ) {
			result.put( rangesInOrder.get( i ), (long) (Integer) facetResult.labelValues[i].value );
		}

		return result;
	}

	public static class Builder<F, E extends Number, K>
		extends AbstractLuceneBucketAggregation.AbstractBuilder<Range<K>, Long>
		implements RangeAggregationBuilder<K> {

		private final DslConverter<?, ? extends F> toFieldValueConverter;
		private final AbstractLuceneNumericFieldCodec<F, E> codec;

		private final List<Range<K>> rangesInOrder = new ArrayList<>();
		private final List<Range<E>> encodedRangesInOrder = new ArrayList<>();

		public Builder(LuceneSearchContext searchContext, String nestedDocumentPath, String absoluteFieldPath,
			DslConverter<?, ? extends F> toFieldValueConverter,
			AbstractLuceneNumericFieldCodec<F, E> codec) {
			super( searchContext, absoluteFieldPath, nestedDocumentPath );
			this.toFieldValueConverter = toFieldValueConverter;
			this.codec = codec;
		}

		@Override
		public void range(Range<? extends K> range) {
			rangesInOrder.add( range.map( Function.identity() ) );
			encodedRangesInOrder.add( range.map( this::convertAndEncode ) );
		}

		@Override
		public LuceneNumericRangeAggregation<F, E, K> build() {
			return new LuceneNumericRangeAggregation<>( this );
		}

		private E convertAndEncode(K value) {
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
