/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationExtractContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationRequestContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueModel;

import org.apache.lucene.index.IndexReader;

/**
 * @param <F> The type of field values exposed to the mapper.
 * @param <T> The type of terms returned by the Lucene Facets.
 * @param <K> The type of keys in the returned map. It can be {@code F}
 * or a different type if value converters are used.
 */
public abstract class AbstractLuceneMultivaluedTermsAggregation<F, T, K, V>
		extends AbstractLuceneBucketAggregation<K, Long> {

	protected final ProjectionConverter<V, ? extends K> fromFieldValueConverter;

	protected final BucketOrder order;
	protected final int maxTermCount;
	protected final int minDocCount;

	AbstractLuceneMultivaluedTermsAggregation(AbstractBuilder<F, T, K, V> builder) {
		super( builder );
		this.fromFieldValueConverter = builder.fromFieldValueConverter;
		this.order = builder.order;
		this.maxTermCount = builder.maxTermCount;
		this.minDocCount = builder.minDocCount;
	}

	protected abstract Extractor<Map<K, Long>> extractor(AggregationRequestContext context);

	protected abstract class AbstractExtractor implements Extractor<Map<K, Long>> {
		@Override
		public final Map<K, Long> extract(AggregationExtractContext context) throws IOException {
			List<Bucket<T>> buckets = getTopBuckets( context );

			if ( minDocCount == 0 && buckets.size() < maxTermCount ) {
				Set<T> firstTerms = collectFirstTerms( context.getIndexReader(), order.isTermOrderDescending(), maxTermCount );
				for ( Bucket<T> bucket : buckets ) {
					firstTerms.remove( bucket.term() );
				}
				firstTerms.forEach( term -> buckets.add( new Bucket<>( term, 0 ) ) );
				buckets.sort( order.toBucketComparator( getAscendingTermComparator() ) );
			}

			return toMap( context.fromDocumentValueConvertContext(), buckets );
		}

		abstract Set<T> collectFirstTerms(IndexReader reader, boolean descending, int limit)
				throws IOException;

		abstract Comparator<T> getAscendingTermComparator();

		abstract V termToFieldValue(T key);

		abstract List<Bucket<T>> getTopBuckets(AggregationExtractContext context) throws IOException;

		private Map<K, Long> toMap(FromDocumentValueConvertContext convertContext, List<Bucket<T>> buckets) {
			Map<K, Long> result = new LinkedHashMap<>(); // LinkedHashMap to preserve ordering
			for ( Bucket<T> bucket : buckets ) {
				V decoded = termToFieldValue( bucket.term() );
				K key = fromFieldValueConverter.fromDocumentValue( decoded, convertContext );
				result.put( key, bucket.count() );
			}
			return result;
		}
	}

	abstract static class AbstractTypeSelector<F, E> implements TermsAggregationBuilder.TypeSelector {
		protected final LuceneSearchIndexScope<?> scope;
		protected final LuceneSearchIndexValueFieldContext<F> field;

		protected AbstractTypeSelector(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			this.scope = scope;
			this.field = field;
		}

		@Override
		public abstract <K> AbstractBuilder<F, ?, K, ?> type(Class<K> expectedType, ValueModel valueModel);
	}

	abstract static class AbstractBuilder<F, T, K, V>
			extends AbstractLuceneBucketAggregation.AbstractBuilder<K, Long>
			implements TermsAggregationBuilder<K> {

		private final ProjectionConverter<V, ? extends K> fromFieldValueConverter;

		private BucketOrder order = BucketOrder.COUNT_DESC;
		private int minDocCount = 1;
		private int maxTermCount = 100;

		AbstractBuilder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field,
				ProjectionConverter<V, ? extends K> fromFieldValueConverter) {
			super( scope, field );
			this.fromFieldValueConverter = fromFieldValueConverter;
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
		public abstract AbstractLuceneMultivaluedTermsAggregation<F, T, K, V> build();

		protected final void order(BucketOrder order) {
			this.order = order;
		}
	}

}
