/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;

import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.sortedset.DefaultSortedSetDocValuesReaderState;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetCounts;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesReaderState;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesReaderState.OrdRange;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.SortedSetDocValues;

/**
 * @param <K> The type of keys in the returned map. It can be {@code String}
 * or a different type if value converters are used.
 */
public class LuceneTextTermsAggregation<K>
		extends AbstractLuceneFacetsBasedTermsAggregation<String, String, K> {

	private static final Comparator<String> STRING_COMPARATOR = Comparator.naturalOrder();

	private LuceneTextTermsAggregation(Builder<K> builder) {
		super( builder );
	}

	@Override
	FacetResult getTopChildren(IndexReader reader, FacetsCollector facetsCollector, int limit)
			throws IOException {
		// May throw IllegalArgumentException
		SortedSetDocValuesReaderState docValuesReaderState = new DefaultSortedSetDocValuesReaderState( reader );

		SortedSetDocValuesFacetCounts facetCounts = new SortedSetDocValuesFacetCounts(
				docValuesReaderState, facetsCollector
		);

		String absoluteFieldPath = getAbsoluteFieldPath();
		// May throw IllegalArgumentException
		return facetCounts.getTopChildren( limit, absoluteFieldPath );
	}

	@Override
	Set<String> collectFirstTerms(IndexReader reader, boolean descending, int limit)
			throws IOException {
		Set<String> collectedTerms = new LinkedHashSet<>();
		String absoluteFieldPath = getAbsoluteFieldPath();

		SortedSetDocValuesReaderState docValuesReaderState = new DefaultSortedSetDocValuesReaderState( reader );
		OrdRange ordRange = docValuesReaderState.getOrdRange( absoluteFieldPath );
		SortedSetDocValues docValues = docValuesReaderState.getDocValues();

		// Note ordRange.end is inclusive, hence the weird index operations.
		if ( descending ) {
			int start = Math.max( ordRange.start, ordRange.end - limit + 1 );
			for ( int i = ordRange.end; i >= start ; --i ) {
				collectedTerms.add( lookupOrd( docValues, i ) );
			}
		}
		else {
			int end = Math.min( ordRange.start + limit - 1, ordRange.end );
			for ( int i = ordRange.start; i <= end; ++i ) {
				collectedTerms.add( lookupOrd( docValues, i ) );
			}
		}

		return collectedTerms;
	}

	private String lookupOrd(SortedSetDocValues docValues, int ord) throws IOException {
		String pathAsString = docValues.lookupOrd( ord ).utf8ToString();
		// FacetsConfig does not store the term directly: it prepends the field name
		String[] pathAsComponents = FacetsConfig.stringToPath( pathAsString );
		return pathAsComponents[1];
	}

	@Override
	Comparator<String> getAscendingTermComparator() {
		return STRING_COMPARATOR;
	}

	@Override
	String labelToTerm(String label) {
		return label;
	}

	@Override
	String termToFieldValue(String key) {
		return key;
	}

	public static class Builder<K>
			extends AbstractBuilder<String, String, K> {

		public Builder(LuceneSearchContext searchContext, String nestedDocumentPath, String absoluteFieldPath,
				ProjectionConverter<? super String, ? extends K> fromFieldValueConverter) {
			super( searchContext, nestedDocumentPath, absoluteFieldPath, fromFieldValueConverter );
		}

		@Override
		public LuceneTextTermsAggregation<K> build() {
			return new LuceneTextTermsAggregation<>( this );
		}

	}

}
