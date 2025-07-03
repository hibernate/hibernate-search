/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.BaseTermsCollector;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.NumericTermsCollector;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.NumericTermsCollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.JoiningLongMultiValuesSource;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationExtractContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationRequestContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.LuceneSearchAggregation;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueModel;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.DocIdSetIterator;

/**
 * @param <F> The type of field values.
 * @param <K> The type of keys in the returned map. It can be {@code F}
 * or a different type if value converters are used.
 */
public class LuceneNumericTermsAggregation<F, E extends Number, K, V, R>
		extends AbstractLuceneMultivaluedTermsAggregation<F, E, K, V, R> {

	private final LuceneNumericDomain<E> numericDomain;

	private final Comparator<E> termComparator;
	private final Function<E, V> decoder;
	private CollectorKey<NumericTermsCollector, NumericTermsCollector> collectorKey;

	private LuceneNumericTermsAggregation(Builder<F, E, K, V, R> builder) {
		super( builder );
		this.numericDomain = builder.codec.getDomain();
		this.termComparator = numericDomain.createComparator();
		this.decoder = builder.decoder;
	}

	@Override
	public Extractor<Map<K, R>> request(AggregationRequestContext context) {
		NestedDocsProvider nestedDocsProvider = createNestedDocsProvider( context );
		JoiningLongMultiValuesSource source = JoiningLongMultiValuesSource.fromLongField(
				absoluteFieldPath, nestedDocsProvider
		);
		LocalAggregationRequestContext localAggregationContext = new LocalAggregationRequestContext( context );
		Extractor<R> extractor = aggregation.request( localAggregationContext );

		var termsCollectorFactory =
				NumericTermsCollectorFactory.instance( source, localAggregationContext.localCollectorFactories() );
		context.requireCollector( termsCollectorFactory );
		collectorKey = termsCollectorFactory.getCollectorKey();

		return new LuceneNumericTermsAggregationExtractor( extractor );
	}

	public static class Factory<F, E extends Number>
			extends
			AbstractLuceneCodecAwareSearchQueryElementFactory<TermsAggregationBuilder.TypeSelector,
					F,
					AbstractLuceneNumericFieldCodec<F, E>> {
		public Factory(AbstractLuceneNumericFieldCodec<F, E> codec) {
			super( codec );
		}

		@Override
		public TermsAggregationBuilder.TypeSelector create(LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<F> field) {
			return new TypeSelector<>( codec, scope, field );
		}
	}

	private class LuceneNumericTermsAggregationExtractor extends AbstractExtractor {

		private LuceneNumericTermsAggregationExtractor(Extractor<R> extractor) {
			super( extractor );
		}

		@Override
		protected BaseTermsCollector termsCollector(AggregationExtractContext context) throws IOException {
			return context.getCollectorResults( collectorKey );
		}

		@Override
		Comparator<E> getAscendingTermComparator() {
			return termComparator;
		}

		@Override
		V termToFieldValue(E key) {
			return decoder.apply( key );
		}

		@Override
		List<Bucket<E, R>> getTopBuckets(AggregationExtractContext context) throws IOException {
			var termsCollector = context.getCollectorResults( collectorKey );

			LocalAggregationExtractContext localContext = new LocalAggregationExtractContext( context );

			List<LongBucket> counts = termsCollector.counts( order, maxTermCount, minDocCount );
			List<Bucket<E, R>> buckets = new ArrayList<>();
			for ( LongBucket bucket : counts ) {
				localContext.setResults( prepareResults( bucket, termsCollector ) );
				buckets.add(
						new Bucket<>(
								numericDomain.sortedDocValueToTerm( bucket.termOrd() ),
								bucket.count(),
								extractor.extract( localContext )
						)
				);
			}
			return buckets;
		}

		@Override
		Set<E> collectFirstTerms(IndexReader reader, boolean descending, int limit)
				throws IOException {
			SortedSet<E> collectedTerms = new TreeSet<>( descending ? termComparator.reversed() : termComparator );
			for ( LeafReaderContext leaf : reader.leaves() ) {
				final LeafReader atomicReader = leaf.reader();
				SortedNumericDocValues docValues = atomicReader.getSortedNumericDocValues( absoluteFieldPath );
				if ( docValues == null ) {
					continue;
				}
				while ( docValues.nextDoc() != DocIdSetIterator.NO_MORE_DOCS ) {
					for ( int i = 0; i < docValues.docValueCount(); i++ ) {
						E term = numericDomain.sortedDocValueToTerm( docValues.nextValue() );
						collectedTerms.add( term );
						// Try not to keep too many terms in memory
						if ( collectedTerms.size() > limit ) {
							collectedTerms.remove( collectedTerms.last() );
						}
					}
				}
			}
			return collectedTerms;
		}

	}

	private static class TypeSelector<F, E extends Number> extends AbstractTypeSelector<F> {
		private final AbstractLuceneNumericFieldCodec<F, E> codec;

		private TypeSelector(AbstractLuceneNumericFieldCodec<F, E> codec,
				LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			super( scope, field );
			this.codec = codec;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <K> Builder<F, ?, K, ?, Long> type(Class<K> expectedType, ValueModel valueModel) {
			if ( ValueModel.RAW.equals( valueModel ) ) {
				return new CountBuilder<>( codec, scope, field,
						( (ProjectionConverter<E, ?>) field.type().rawProjectionConverter() )
								.withConvertedType( expectedType, field ),
						Function.identity()
				);
			}
			else {
				return new CountBuilder<>( codec, scope, field,
						field.type().projectionConverter( valueModel ).withConvertedType( expectedType, field ),
						codec::decode
				);
			}
		}
	}

	private static class CountBuilder<F, E extends Number, K, V> extends Builder<F, E, K, V, Long> {

		private CountBuilder(AbstractLuceneNumericFieldCodec<F, E> codec, LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<F> field,
				ProjectionConverter<V, ? extends K> fromFieldValueConverter,
				Function<E, V> decoder) {
			super( codec, scope, field, LuceneSearchAggregation.from( scope,
					LuceneCountDocumentAggregation.factory().create( scope, null ).type().build() ), fromFieldValueConverter,
					decoder );
		}
	}

	private static class Builder<F, E extends Number, K, V, R>
			extends AbstractBuilder<F, E, K, V, R> {

		private final AbstractLuceneNumericFieldCodec<F, E> codec;
		private final Function<E, V> decoder;

		private Builder(AbstractLuceneNumericFieldCodec<F, E> codec, LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<F> field, LuceneSearchAggregation<R> aggregation,
				ProjectionConverter<V, ? extends K> fromFieldValueConverter,
				Function<E, V> decoder) {
			super( scope, field, aggregation, fromFieldValueConverter );
			this.codec = codec;
			this.decoder = decoder;
		}

		private Builder(AbstractLuceneNumericFieldCodec<F, E> codec, LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<?> field,
				LuceneSearchAggregation<R> aggregation, ProjectionConverter<V, ? extends K> fromFieldValueConverter,
				Function<E, V> decoder,
				BucketOrder order, int minDocCount, int maxTermCount) {
			super( scope, field, aggregation, fromFieldValueConverter, order, minDocCount, maxTermCount );
			this.codec = codec;
			this.decoder = decoder;
		}

		@Override
		public LuceneNumericTermsAggregation<F, E, K, V, R> build() {
			return new LuceneNumericTermsAggregation<>( this );
		}

		@Override
		public <T> TermsAggregationBuilder<K, T> withValue(SearchAggregation<T> aggregation) {
			return new Builder<>( codec, scope, field, LuceneSearchAggregation.from( scope, aggregation ),
					fromFieldValueConverter, decoder, order, minDocCount, maxTermCount );
		}
	}
}
