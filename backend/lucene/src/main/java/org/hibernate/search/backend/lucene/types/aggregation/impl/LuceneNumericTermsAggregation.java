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

import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueConvert;

import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
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
public class LuceneNumericTermsAggregation<F, E extends Number, K>
		extends AbstractLuceneFacetsBasedTermsAggregation<F, E, K> {

	private final AbstractLuceneNumericFieldCodec<F, E> codec;
	private final LuceneNumericDomain<E> numericDomain;

	private final Comparator<E> termComparator;

	private LuceneNumericTermsAggregation(Builder<F, E, K> builder) {
		super( builder );
		this.codec = builder.codec;
		this.numericDomain = codec.getDomain();
		this.termComparator = numericDomain.createComparator();
	}

	@Override
	FacetResult getTopChildren(IndexReader reader, FacetsCollector facetsCollector,
			NestedDocsProvider nestedDocsProvider, int limit)
			throws IOException {
		Facets facetCounts = numericDomain.createTermsFacetCounts(
				absoluteFieldPath, facetsCollector, nestedDocsProvider
		);
		return facetCounts.getTopChildren( limit, absoluteFieldPath );
	}

	@Override
	SortedSet<E> collectFirstTerms(IndexReader reader, boolean descending, int limit)
			throws IOException {
		TreeSet<E> collectedTerms = new TreeSet<>( descending ? termComparator.reversed() : termComparator );
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

	@Override
	Comparator<E> getAscendingTermComparator() {
		return termComparator;
	}

	@Override
	E labelToTerm(String termAsString) {
		return numericDomain.sortedDocValueToTerm( Long.parseLong( termAsString ) );
	}

	@Override
	F termToFieldValue(E term) {
		return codec.decode( term );
	}

	public static class Factory<F>
			extends
			AbstractLuceneCodecAwareSearchQueryElementFactory<TermsAggregationBuilder.TypeSelector,
					F,
					AbstractLuceneNumericFieldCodec<F, ?>> {
		public Factory(AbstractLuceneNumericFieldCodec<F, ?> codec) {
			super( codec );
		}

		@Override
		public TypeSelector<?> create(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			return new TypeSelector<>( codec, scope, field );
		}
	}

	private static class TypeSelector<F> extends AbstractTypeSelector<F> {
		private final AbstractLuceneNumericFieldCodec<F, ?> codec;

		private TypeSelector(AbstractLuceneNumericFieldCodec<F, ?> codec,
				LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			super( scope, field );
			this.codec = codec;
		}

		@Override
		public <K> Builder<F, ?, K> type(Class<K> expectedType, ValueConvert convert) {
			return new Builder<>( codec, scope, field,
					field.type().projectionConverter( convert ).withConvertedType( expectedType, field ) );
		}
	}

	private static class Builder<F, E extends Number, K>
			extends AbstractBuilder<F, E, K> {

		private final AbstractLuceneNumericFieldCodec<F, E> codec;

		public Builder(AbstractLuceneNumericFieldCodec<F, E> codec, LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<F> field, ProjectionConverter<F, ? extends K> fromFieldValueConverter) {
			super( scope, field, fromFieldValueConverter );
			this.codec = codec;
		}

		@Override
		public LuceneNumericTermsAggregation<F, E, K> build() {
			return new LuceneNumericTermsAggregation<>( this );
		}
	}

}
