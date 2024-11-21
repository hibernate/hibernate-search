/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationRequestContext;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueModel;

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
public class LuceneNumericTermsAggregation<F, E extends Number, K, V>
		extends AbstractLuceneFacetsBasedTermsAggregation<F, E, K, V> {

	private final LuceneNumericDomain<E> numericDomain;

	private final Comparator<E> termComparator;
	private final Function<E, V> decoder;

	private LuceneNumericTermsAggregation(Builder<F, E, K, V> builder) {
		super( builder );
		this.numericDomain = builder.codec.getDomain();
		this.termComparator = numericDomain.createComparator();
		this.decoder = builder.decoder;
	}

	@Override
	protected Extractor<Map<K, Long>> extractor(AggregationRequestContext context) {
		return new LuceneNumericTermsAggregationExtractor();
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
		V termToFieldValue(E term) {
			return decoder.apply( term );
		}
	}

	private static class TypeSelector<F, E extends Number> extends AbstractTypeSelector<F, E> {
		private final AbstractLuceneNumericFieldCodec<F, E> codec;

		private TypeSelector(AbstractLuceneNumericFieldCodec<F, E> codec,
				LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			super( scope, field );
			this.codec = codec;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <K> Builder<F, ?, K, ?> type(Class<K> expectedType, ValueModel valueModel) {
			if ( ValueModel.RAW.equals( valueModel ) ) {
				return new Builder<>( codec, scope, field,
						( (ProjectionConverter<E, ?>) field.type().rawProjectionConverter() )
								.withConvertedType( expectedType, field ),
						Function.identity()
				);
			}
			else {
				return new Builder<>( codec, scope, field,
						field.type().projectionConverter( valueModel ).withConvertedType( expectedType, field ),
						codec::decode
				);
			}
		}
	}

	private static class Builder<F, E extends Number, K, V>
			extends AbstractBuilder<F, E, K, V> {

		private final AbstractLuceneNumericFieldCodec<F, E> codec;
		private final Function<E, V> decoder;

		public Builder(AbstractLuceneNumericFieldCodec<F, E> codec, LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<F> field, ProjectionConverter<V, ? extends K> fromFieldValueConverter,
				Function<E, V> decoder) {
			super( scope, field, fromFieldValueConverter );
			this.codec = codec;
			this.decoder = decoder;
		}

		@Override
		public LuceneNumericTermsAggregation<F, E, K, V> build() {
			return new LuceneNumericTermsAggregation<>( this );
		}
	}

}
