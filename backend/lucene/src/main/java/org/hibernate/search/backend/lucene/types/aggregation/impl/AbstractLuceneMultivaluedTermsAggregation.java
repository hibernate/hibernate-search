/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.BaseTermsCollector;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationExtractContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.LuceneSearchAggregation;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueModel;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.CollectorManager;

/**
 * @param <F> The type of field values exposed to the mapper.
 * @param <T> The type of terms returned by the Lucene Facets.
 * @param <K> The type of keys in the returned map. It can be {@code F}
 * or a different type if value converters are used.
 */
public abstract class AbstractLuceneMultivaluedTermsAggregation<F, T, K, V, R>
		extends AbstractLuceneBucketAggregation<K, R> {

	protected final ProjectionConverter<V, ? extends K> fromFieldValueConverter;

	protected final BucketOrder order;
	protected final int maxTermCount;
	protected final int minDocCount;
	protected final LuceneSearchAggregation<R> aggregation;

	AbstractLuceneMultivaluedTermsAggregation(AbstractBuilder<F, T, K, V, R> builder) {
		super( builder );
		this.fromFieldValueConverter = builder.fromFieldValueConverter;
		this.order = builder.order;
		this.maxTermCount = builder.maxTermCount;
		this.minDocCount = builder.minDocCount;
		this.aggregation = builder.aggregation;
	}

	protected abstract class AbstractExtractor implements Extractor<Map<K, R>> {
		protected final Extractor<R> extractor;

		protected AbstractExtractor(Extractor<R> extractor) {
			this.extractor = extractor;
		}

		@Override
		public final Map<K, R> extract(AggregationExtractContext context) throws IOException {
			List<Bucket<T, R>> buckets = getTopBuckets( context );

			if ( minDocCount == 0 && buckets.size() < maxTermCount ) {
				Set<T> firstTerms = collectFirstTerms( context.getIndexReader(), order.isTermOrderDescending(), maxTermCount );
				for ( Bucket<T, R> bucket : buckets ) {
					firstTerms.remove( bucket.term() );
				}
				R zeroValue = createZeroValue( context );
				firstTerms.forEach( term -> buckets.add( new Bucket<>( term, 0, zeroValue ) ) );
				buckets.sort( order.toBucketComparator( getAscendingTermComparator() ) );
			}

			return toMap( context.fromDocumentValueConvertContext(), buckets );
		}

		protected abstract BaseTermsCollector termsCollector(AggregationExtractContext context) throws IOException;

		protected R createZeroValue(AggregationExtractContext context) throws IOException {
			LocalAggregationExtractContext localContext = new LocalAggregationExtractContext( context );
			var termsCollector = termsCollector( context );
			CollectorManager<Collector, ?>[] managers = termsCollector.managers();
			CollectorKey<?, ?>[] keys = termsCollector.keys();
			HashMap<CollectorKey<?, ?>, Object> results = new HashMap<>();
			for ( int i = 0; i < keys.length; i++ ) {
				results.put( keys[i], managers[i].reduce( List.of( managers[i].newCollector() ) ) );
			}
			localContext.setResults( results );
			return extractor.extract( localContext );
		}

		abstract Set<T> collectFirstTerms(IndexReader reader, boolean descending, int limit)
				throws IOException;

		abstract Comparator<T> getAscendingTermComparator();

		abstract V termToFieldValue(T key);

		abstract List<Bucket<T, R>> getTopBuckets(AggregationExtractContext context) throws IOException;

		private Map<K, R> toMap(FromDocumentValueConvertContext convertContext, List<Bucket<T, R>> buckets) {
			Map<K, R> result = new LinkedHashMap<>(); // LinkedHashMap to preserve ordering
			for ( Bucket<T, R> bucket : buckets ) {
				V decoded = termToFieldValue( bucket.term() );
				K key = fromFieldValueConverter.fromDocumentValue( decoded, convertContext );
				result.put( key, bucket.value() );
			}
			return result;
		}

		protected Map<CollectorKey<?, ?>, Object> prepareResults(LongBucket bucket, BaseTermsCollector termsCollector)
				throws IOException {
			Map<CollectorKey<?, ?>, Object> result = new HashMap<>();
			List<Collector>[] collectors = bucket.collectors;
			CollectorKey<?, ?>[] collectorKeys = termsCollector.keys();
			CollectorManager<Collector, ?>[] managers = termsCollector.managers();
			for ( int i = 0; i < collectorKeys.length; i++ ) {
				result.put( collectorKeys[i], managers[i].reduce( collectors[i] ) );
			}
			return result;
		}
	}

	abstract static class AbstractTypeSelector<F> implements TermsAggregationBuilder.TypeSelector {
		protected final LuceneSearchIndexScope<?> scope;
		protected final LuceneSearchIndexValueFieldContext<F> field;

		protected AbstractTypeSelector(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			this.scope = scope;
			this.field = field;
		}

		@Override
		public abstract <K> AbstractBuilder<F, ?, K, ?, Long> type(Class<K> expectedType, ValueModel valueModel);
	}

	abstract static class AbstractBuilder<F, T, K, V, R>
			extends AbstractLuceneBucketAggregation.AbstractBuilder<K, R>
			implements TermsAggregationBuilder<K, R> {

		protected final LuceneSearchAggregation<R> aggregation;
		protected final ProjectionConverter<V, ? extends K> fromFieldValueConverter;
		protected BucketOrder order;
		protected int minDocCount;
		protected int maxTermCount;

		AbstractBuilder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field,
				LuceneSearchAggregation<R> aggregation, ProjectionConverter<V, ? extends K> fromFieldValueConverter) {
			this( scope, field, aggregation, fromFieldValueConverter, BucketOrder.COUNT_DESC, 1, 100 );
		}

		AbstractBuilder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<?> field,
				LuceneSearchAggregation<R> aggregation, ProjectionConverter<V, ? extends K> fromFieldValueConverter,
				BucketOrder order, int minDocCount, int maxTermCount) {
			super( scope, field );
			this.aggregation = aggregation;
			this.fromFieldValueConverter = fromFieldValueConverter;
			this.order = order;
			this.minDocCount = minDocCount;
			this.maxTermCount = maxTermCount;
		}

		@Override
		public void orderByCountDescending() {
			order( BucketOrder.COUNT_DESC );
		}

		@Override
		public void orderByCountAscending() {
			order( BucketOrder.COUNT_ASC );
		}

		@Override
		public void orderByTermAscending() {
			order( BucketOrder.TERM_ASC );
		}

		@Override
		public void orderByTermDescending() {
			order( BucketOrder.TERM_DESC );
		}

		@Override
		public void minDocumentCount(int minDocumentCount) {
			this.minDocCount = minDocumentCount;
		}

		@Override
		public void maxTermCount(int maxTermCount) {
			this.maxTermCount = maxTermCount;
		}

		@Override
		public abstract AbstractLuceneMultivaluedTermsAggregation<F, T, K, V, R> build();

		protected final void order(BucketOrder order) {
			this.order = order;
		}
	}

}
