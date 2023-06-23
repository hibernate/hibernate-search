/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.FacetsCollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationExtractContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationRequestContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueConvert;

import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.index.IndexReader;

/**
 * @param <F> The type of field values exposed to the mapper.
 * @param <T> The type of terms returned by the Lucene Facets.
 * @param <K> The type of keys in the returned map. It can be {@code F}
 * or a different type if value converters are used.
 */
public abstract class AbstractLuceneFacetsBasedTermsAggregation<F, T, K>
		extends AbstractLuceneBucketAggregation<K, Long> {

	private final ProjectionConverter<F, ? extends K> fromFieldValueConverter;

	private final BucketOrder order;
	private final int maxTermCount;
	private final int minDocCount;

	AbstractLuceneFacetsBasedTermsAggregation(AbstractBuilder<F, T, K> builder) {
		super( builder );
		this.fromFieldValueConverter = builder.fromFieldValueConverter;
		this.order = builder.order;
		this.maxTermCount = builder.maxTermCount;
		this.minDocCount = builder.minDocCount;
	}

	@Override
	public void request(AggregationRequestContext context) {
		context.requireCollector( FacetsCollectorFactory.INSTANCE );
	}

	@Override
	public final Map<K, Long> extract(AggregationExtractContext context) throws IOException {
		FromDocumentValueConvertContext convertContext = context.fromDocumentValueConvertContext();

		List<Bucket<T>> buckets = getTopBuckets( context );

		if ( BucketOrder.COUNT_DESC.equals( order ) && ( minDocCount > 0 || buckets.size() >= maxTermCount ) ) {
			/*
			 * Optimization: in this case, minDocCount and sorting can be safely ignored.
			 * We already have all the buckets we need, and they are already sorted.
			 */
			return toMap( convertContext, buckets );
		}

		if ( minDocCount <= 0 ) {
			Set<T> firstTerms = collectFirstTerms( context.getIndexReader(), order.isTermOrderDescending(), maxTermCount );
			// If some of the first terms are already in non-zero buckets, ignore them in the next step
			for ( Bucket<T> bucket : buckets ) {
				firstTerms.remove( bucket.term );
			}
			// Complete the list of buckets with zero-count terms
			for ( T term : firstTerms ) {
				buckets.add( new Bucket<>( term, 0L ) );
			}
		}

		// Sort the list of buckets and trim it if necessary (there may be more buckets than we want in some cases)
		buckets.sort( order.toBucketComparator( getAscendingTermComparator() ) );
		if ( buckets.size() > maxTermCount ) {
			buckets.subList( maxTermCount, buckets.size() ).clear();
		}

		return toMap( convertContext, buckets );
	}

	abstract FacetResult getTopChildren(IndexReader reader, FacetsCollector facetsCollector,
			NestedDocsProvider nestedDocsProvider, int limit)
			throws IOException;

	abstract Set<T> collectFirstTerms(IndexReader reader, boolean descending, int limit)
			throws IOException;

	abstract Comparator<T> getAscendingTermComparator();

	abstract T labelToTerm(String label);

	abstract F termToFieldValue(T key);

	private List<Bucket<T>> getTopBuckets(AggregationExtractContext context) throws IOException {
		FacetsCollector facetsCollector = context.getCollector( FacetsCollectorFactory.KEY );

		NestedDocsProvider nestedDocsProvider = createNestedDocsProvider( context );

		/*
		 * TODO HSEARCH-3666 What if the sort order is by term value?
		 *  Lucene returns facets in descending count order.
		 *  If that's what we need, then we can ask Lucene to apply the "maxTermCount" limit directly.
		 *  This is what we do here.
		 *  But if we need a different sort, then having to retrieve the "top N" facets by document count
		 *  becomes clearly sub-optimal: to properly implement this, we would need to retrieve
		 *  *all* facets, and Lucene would allocate an array of Integer.MAX_VALUE elements.
		 *  To improve on this, we would need to re-implement the facet collections.
		 */
		int limit = maxTermCount;
		FacetResult facetResult = getTopChildren( context.getIndexReader(), facetsCollector, nestedDocsProvider, limit );

		List<Bucket<T>> buckets = new ArrayList<>();

		if ( facetResult != null ) {
			// Add results for matching documents
			for ( LabelAndValue labelAndValue : facetResult.labelValues ) {
				long count = (Integer) labelAndValue.value;
				if ( count >= minDocCount ) {
					buckets.add( new Bucket<>( labelToTerm( labelAndValue.label ), count ) );
				}
			}
		}

		return buckets;
	}

	private Map<K, Long> toMap(FromDocumentValueConvertContext convertContext, List<Bucket<T>> buckets) {
		Map<K, Long> result = new LinkedHashMap<>(); // LinkedHashMap to preserve ordering
		for ( Bucket<T> bucket : buckets ) {
			F decoded = termToFieldValue( bucket.term );
			K key = fromFieldValueConverter.fromDocumentValue( decoded, convertContext );
			result.put( key, bucket.count );
		}
		return result;
	}

	abstract static class AbstractTypeSelector<F> implements TermsAggregationBuilder.TypeSelector {
		protected final LuceneSearchIndexScope<?> scope;
		protected final LuceneSearchIndexValueFieldContext<F> field;

		protected AbstractTypeSelector(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			this.scope = scope;
			this.field = field;
		}

		@Override
		public abstract <K> AbstractBuilder<F, ?, K> type(Class<K> expectedType, ValueConvert convert);
	}

	abstract static class AbstractBuilder<F, T, K>
			extends AbstractLuceneBucketAggregation.AbstractBuilder<K, Long>
			implements TermsAggregationBuilder<K> {

		private final ProjectionConverter<F, ? extends K> fromFieldValueConverter;

		private BucketOrder order = BucketOrder.COUNT_DESC;
		private int minDocCount = 1;
		private int maxTermCount = 100;

		AbstractBuilder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field,
				ProjectionConverter<F, ? extends K> fromFieldValueConverter) {
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
		public abstract AbstractLuceneFacetsBasedTermsAggregation<F, T, K> build();

		protected final void order(BucketOrder order) {
			this.order = order;
		}
	}

}
