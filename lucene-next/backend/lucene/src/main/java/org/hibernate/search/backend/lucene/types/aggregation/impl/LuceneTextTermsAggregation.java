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
import java.util.TreeSet;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.TermResults;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.TextTermsCollector;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.TextTermsCollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.JoiningTextMultiValuesSource;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationExtractContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationRequestContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.LuceneSearchAggregation;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneValueFieldSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueModel;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.MultiDocValues;
import org.apache.lucene.index.SortedSetDocValues;

/**
 * @param <K> The type of keys in the returned map. It can be {@code String}
 * or a different type if value converters are used.
 */
public class LuceneTextTermsAggregation<K, R>
		extends AbstractLuceneMultivaluedTermsAggregation<String, String, K, String, R> {

	private static final Comparator<String> STRING_COMPARATOR = Comparator.naturalOrder();

	private CollectorKey<TextTermsCollector, TermResults> collectorKey;

	private LuceneTextTermsAggregation(Builder<K, R> builder) {
		super( builder );
	}

	@Override
	public Extractor<Map<K, R>> request(AggregationRequestContext context) {
		NestedDocsProvider nestedDocsProvider = createNestedDocsProvider( context );
		JoiningTextMultiValuesSource source = JoiningTextMultiValuesSource.fromField(
				absoluteFieldPath, nestedDocsProvider
		);

		LocalAggregationRequestContext localAggregationContext = new LocalAggregationRequestContext( context );
		Extractor<R> extractor = aggregation.request( localAggregationContext );

		var termsCollectorFactory = TextTermsCollectorFactory.instance( absoluteFieldPath, source,
				localAggregationContext.localCollectorFactories() );
		context.requireCollector( termsCollectorFactory );
		collectorKey = termsCollectorFactory.getCollectorKey();

		return new LuceneTextTermsAggregationExtractor( extractor );
	}

	private class LuceneTextTermsAggregationExtractor extends AbstractExtractor {

		private LuceneTextTermsAggregationExtractor(Extractor<R> extractor) {
			super( extractor );
		}

		@Override
		protected TermResults termResults(AggregationExtractContext context) throws IOException {
			return context.getCollectorResults( collectorKey );
		}

		@Override
		Set<String> collectFirstTerms(IndexReader reader, boolean descending, int limit)
				throws IOException {
			TreeSet<String> collectedTerms = new TreeSet<>(
					descending ? STRING_COMPARATOR.reversed() : STRING_COMPARATOR );
			for ( LeafReaderContext leaf : reader.leaves() ) {
				final LeafReader atomicReader = leaf.reader();
				SortedSetDocValues docValues = atomicReader.getSortedSetDocValues( absoluteFieldPath );
				if ( docValues == null ) {
					continue;
				}
				int valueCount = (int) docValues.getValueCount();
				if ( descending ) {
					int start = Math.max( 0, valueCount - limit );
					for ( int i = start; i < valueCount; ++i ) {
						collectedTerms.add( docValues.lookupOrd( i ).utf8ToString() );
					}
				}
				else {
					int end = Math.min( limit, valueCount );
					for ( int i = 0; i < end; ++i ) {
						collectedTerms.add( docValues.lookupOrd( i ).utf8ToString() );
					}
				}
			}
			return collectedTerms;
		}

		@Override
		Comparator<String> getAscendingTermComparator() {
			return STRING_COMPARATOR;
		}

		@Override
		String termToFieldValue(String key) {
			return key;
		}

		@Override
		List<Bucket<String, R>> getTopBuckets(AggregationExtractContext context) throws IOException {
			var termResults = context.getCollectorResults( collectorKey );

			LocalAggregationExtractContext localContext = new LocalAggregationExtractContext( context );

			List<LongBucket> results = termResults.counts( order, maxTermCount, minDocCount );

			var dv = MultiDocValues.getSortedSetValues( context.getIndexReader(), absoluteFieldPath );
			List<Bucket<String, R>> buckets = new ArrayList<>();
			for ( LongBucket bucket : results ) {
				localContext.setResults( prepareResults( bucket, termResults ) );
				buckets.add(
						new Bucket<>(
								dv.lookupOrd( bucket.termOrd() ).utf8ToString(),
								bucket.count(),
								extractor.extract( localContext )
						)
				);
			}
			return buckets;
		}
	}

	public static class Factory
			extends AbstractLuceneValueFieldSearchQueryElementFactory<TermsAggregationBuilder.TypeSelector, String> {
		@Override
		public TypeSelector create(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<String> field) {
			return new TypeSelector( scope, field );
		}
	}

	private static class TypeSelector extends AbstractTypeSelector<String> {
		private TypeSelector(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<String> field) {
			super( scope, field );
		}

		@SuppressWarnings("unchecked")
		@Override
		public <K> Builder<K, Long> type(Class<K> expectedType, ValueModel valueModel) {
			if ( ValueModel.RAW.equals( valueModel ) ) {
				return new CountBuilder<>( scope, field,
						( (ProjectionConverter<String, ?>) field.type().rawProjectionConverter() )
								.withConvertedType( expectedType, field )
				);
			}
			else {
				return new CountBuilder<>( scope, field,
						field.type().projectionConverter( valueModel ).withConvertedType( expectedType, field ) );
			}
		}
	}

	private static class CountBuilder<K> extends Builder<K, Long> {

		private CountBuilder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<String> field,
				ProjectionConverter<String, ? extends K> fromFieldValueConverter) {
			super( scope, field,
					LuceneSearchAggregation.from( scope,
							LuceneCountDocumentAggregation.factory().create( scope, null ).type().build() ),
					fromFieldValueConverter );
		}
	}

	private static class Builder<K, V>
			extends AbstractBuilder<String, String, K, String, V> {

		private Builder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<String> field,
				LuceneSearchAggregation<V> aggregation, ProjectionConverter<String, ? extends K> fromFieldValueConverter) {
			super( scope, field, aggregation, fromFieldValueConverter );
		}

		private Builder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<?> field,
				LuceneSearchAggregation<V> aggregation, ProjectionConverter<String, ? extends K> fromFieldValueConverter,
				BucketOrder order, int minDocCount, int maxTermCount) {
			super( scope, field, aggregation, fromFieldValueConverter, order, minDocCount, maxTermCount );
		}

		@Override
		public LuceneTextTermsAggregation<K, V> build() {
			return new LuceneTextTermsAggregation<>( this );
		}

		@Override
		public <T> TermsAggregationBuilder<K, T> withValue(SearchAggregation<T> aggregation) {
			return new Builder<>( scope, field, LuceneSearchAggregation.from( scope, aggregation ), fromFieldValueConverter,
					order, minDocCount, maxTermCount );
		}
	}

}
