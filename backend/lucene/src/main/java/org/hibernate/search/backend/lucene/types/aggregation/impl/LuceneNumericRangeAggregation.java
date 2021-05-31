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
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationExtractContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationRequestContext;
import org.hibernate.search.backend.lucene.search.impl.AbstractLuceneCodecAwareSearchValueFieldQueryElementFactory;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchValueFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueConvert;
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
	}

	@Override
	public Map<Range<K>, Long> extract(AggregationExtractContext context) throws IOException {
		LuceneNumericDomain<E> numericDomain = codec.getDomain();

		FacetsCollector facetsCollector = context.getCollector( FacetsCollectorFactory.KEY );

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

	public static class Factory<F>
			extends AbstractLuceneCodecAwareSearchValueFieldQueryElementFactory<RangeAggregationBuilder.TypeSelector, F, AbstractLuceneNumericFieldCodec<F, ?>> {
		public Factory(AbstractLuceneNumericFieldCodec<F, ?> codec) {
			super( codec );
		}

		@Override
		public TypeSelector<?> create(LuceneSearchContext searchContext, LuceneSearchValueFieldContext<F> field) {
			return new TypeSelector<>( codec, searchContext, field );
		}
	}

	public static class TypeSelector<F> implements RangeAggregationBuilder.TypeSelector {
		private final AbstractLuceneNumericFieldCodec<F, ?> codec;
		private final LuceneSearchContext searchContext;
		private final LuceneSearchValueFieldContext<F> field;

		private TypeSelector(AbstractLuceneNumericFieldCodec<F, ?> codec,
				LuceneSearchContext searchContext, LuceneSearchValueFieldContext<F> field) {
			this.codec = codec;
			this.searchContext = searchContext;
			this.field = field;
		}

		@Override
		public <K> Builder<F, ?, K> type(Class<K> expectedType, ValueConvert convert) {
			return new Builder<>( codec, searchContext, field,
					field.type().dslConverter( convert ).withInputType( expectedType, field ) );
		}
	}

	public static class Builder<F, E extends Number, K>
			extends AbstractLuceneBucketAggregation.AbstractBuilder<Range<K>, Long>
			implements RangeAggregationBuilder<K> {

		private final DslConverter<? super K, F> toFieldValueConverter;
		private final AbstractLuceneNumericFieldCodec<F, E> codec;

		private final List<Range<K>> rangesInOrder = new ArrayList<>();
		private final List<Range<E>> encodedRangesInOrder = new ArrayList<>();

		public Builder(AbstractLuceneNumericFieldCodec<F, E> codec,
				LuceneSearchContext searchContext, LuceneSearchValueFieldContext<?> field,
				DslConverter<? super K, F> toFieldValueConverter) {
			super( searchContext, field );
			this.codec = codec;
			this.toFieldValueConverter = toFieldValueConverter;
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
				F converted = toFieldValueConverter.convert( value, searchContext.toDocumentFieldValueConvertContext() );
				return codec.encode( converted );
			}
			catch (RuntimeException e) {
				throw log.cannotConvertDslParameter( e.getMessage(), e, field.eventContext() );
			}
		}
	}
}
