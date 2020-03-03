/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.io.IOException;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;

import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.LongValueFacetCounts;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.DocIdSetIterator;

/**
 * @param <F> The type of field values.
 * @param <K> The type of keys in the returned map. It can be {@code F}
 * or a different type if value converters are used.
 */
public class LuceneNumericTermsAggregation<F, E extends Number, K>
	extends AbstractLuceneFacetsBasedTermsAggregation<F, Long, K> {

	private static final Comparator<Long> LONG_COMPARATOR = Comparator.naturalOrder();

	private final AbstractLuceneNumericFieldCodec<F, E> codec;
	private final LuceneNumericDomain<E> numericDomain;

	private LuceneNumericTermsAggregation(Builder<F, E, K> builder) {
		super( builder );
		this.codec = builder.codec;
		this.numericDomain = codec.getDomain();
	}

	@Override
	FacetResult getTopChildren(IndexReader reader, FacetsCollector facetsCollector, int limit)
		throws IOException {
		LongValueFacetCounts facetCounts = numericDomain.createTermsFacetCounts(
			absoluteFieldPath, facetsCollector, multiValueMode, nestedDocsProvider );
		return facetCounts.getTopChildren( limit, absoluteFieldPath );
	}

	@Override
	SortedSet<Long> collectFirstTerms(IndexReader reader, boolean descending, int limit)
		throws IOException {
		// TODO HSEARCH-1927 when we switch to Sorted/SortedSetDocValues, this can be implemented in a much more efficient way
		//  since docvalues will be sorted. See the same method in LuceneTextTermsAggregation.
		TreeSet<Long> collectedTerms = new TreeSet<>( descending ? LONG_COMPARATOR.reversed() : LONG_COMPARATOR );
		for ( LeafReaderContext leaf : reader.leaves() ) {
			final LeafReader atomicReader = leaf.reader();
			NumericDocValues docValues = atomicReader.getNumericDocValues( absoluteFieldPath );
			if ( docValues == null ) {
				continue;
			}
			while ( docValues.nextDoc() != DocIdSetIterator.NO_MORE_DOCS ) {
				Long term = docValues.longValue();
				collectedTerms.add( term );
				// Try not to keep too many terms in memory
				if ( collectedTerms.size() > limit ) {
					collectedTerms.remove( collectedTerms.last() );
				}
			}
		}
		return collectedTerms;
	}

	@Override
	Comparator<Long> getAscendingTermComparator() {
		return LONG_COMPARATOR;
	}

	@Override
	Long labelToTerm(String termAsString) {
		return Long.valueOf( termAsString );
	}

	@Override
	F termToFieldValue(Long term) {
		return codec.decode( numericDomain.fromDocValue( term ) );
	}

	public static class Builder<F, E extends Number, K>
		extends AbstractBuilder<F, Long, K> {

		private final AbstractLuceneNumericFieldCodec<F, E> codec;

		public Builder(LuceneSearchContext searchContext, String nestedDocumentPath, String absoluteFieldPath,
			ProjectionConverter<? super F, ? extends K> fromFieldValueConverter,
			AbstractLuceneNumericFieldCodec<F, E> codec) {
			super( searchContext, nestedDocumentPath, absoluteFieldPath, fromFieldValueConverter );
			this.codec = codec;
		}

		@Override
		public LuceneNumericTermsAggregation<F, E, K> build() {
			return new LuceneNumericTermsAggregation<>( this );
		}
	}

}
