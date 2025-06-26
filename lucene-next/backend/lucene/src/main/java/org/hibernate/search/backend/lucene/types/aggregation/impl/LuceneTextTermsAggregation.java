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
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.TextTermsCollector;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.TextTermsCollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.JoiningTextMultiValuesSource;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationExtractContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationRequestContext;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneValueFieldSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
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
public class LuceneTextTermsAggregation<K>
		extends AbstractLuceneMultivaluedTermsAggregation<String, String, K, String> {

	private static final Comparator<String> STRING_COMPARATOR = Comparator.naturalOrder();

	private CollectorKey<TextTermsCollector, TextTermsCollector> collectorKey;

	private LuceneTextTermsAggregation(Builder<K> builder) {
		super( builder );
	}

	@Override
	public Extractor<Map<K, Long>> request(AggregationRequestContext context) {
		NestedDocsProvider nestedDocsProvider = createNestedDocsProvider( context );
		JoiningTextMultiValuesSource source = JoiningTextMultiValuesSource.fromField(
				absoluteFieldPath, nestedDocsProvider
		);
		var termsCollectorFactory = TextTermsCollectorFactory.instance( absoluteFieldPath, source );
		context.requireCollector( termsCollectorFactory );
		collectorKey = termsCollectorFactory.getCollectorKey();

		return extractor( context );
	}

	@Override
	protected Extractor<Map<K, Long>> extractor(AggregationRequestContext context) {
		return new LuceneTextTermsAggregationExtractor();
	}

	private class LuceneTextTermsAggregationExtractor extends AbstractExtractor {

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
		List<Bucket<String>> getTopBuckets(AggregationExtractContext context) throws IOException {
			var termsCollector = context.getCollectorResults( collectorKey );

			List<LongBucket> counts = termsCollector.counts( order, maxTermCount, minDocCount );

			var dv = MultiDocValues.getSortedSetValues( context.getIndexReader(), absoluteFieldPath );
			List<Bucket<String>> buckets = new ArrayList<>();
			for ( LongBucket bucket : counts ) {
				buckets.add( new Bucket<>( dv.lookupOrd( bucket.term() ).utf8ToString(), bucket.count() ) );
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

	private static class TypeSelector extends AbstractTypeSelector<String, String> {
		private TypeSelector(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<String> field) {
			super( scope, field );
		}

		@SuppressWarnings("unchecked")
		@Override
		public <K> Builder<K> type(Class<K> expectedType, ValueModel valueModel) {
			if ( ValueModel.RAW.equals( valueModel ) ) {
				return new Builder<>( scope, field,
						( (ProjectionConverter<String, ?>) field.type().rawProjectionConverter() )
								.withConvertedType( expectedType, field )
				);
			}
			else {
				return new Builder<>( scope, field,
						field.type().projectionConverter( valueModel ).withConvertedType( expectedType, field ) );
			}
		}
	}

	private static class Builder<K>
			extends AbstractBuilder<String, String, K, String> {

		private Builder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<String> field,
				ProjectionConverter<String, ? extends K> fromFieldValueConverter) {
			super( scope, field, fromFieldValueConverter );
		}

		@Override
		public LuceneTextTermsAggregation<K> build() {
			return new LuceneTextTermsAggregation<>( this );
		}

	}

}
