/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.io.IOException;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.JoiningTextMultiValuesSource;
import org.hibernate.search.backend.lucene.lowlevel.facet.impl.TextMultiValueFacetCounts;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneValueFieldSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueConvert;

import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
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
	FacetResult getTopChildren(IndexReader reader, FacetsCollector facetsCollector,
			NestedDocsProvider nestedDocsProvider, int limit)
			throws IOException {
		JoiningTextMultiValuesSource valueSource = JoiningTextMultiValuesSource.fromField(
				absoluteFieldPath, nestedDocsProvider
		);
		TextMultiValueFacetCounts facetCounts = new TextMultiValueFacetCounts(
				reader, absoluteFieldPath, valueSource, facetsCollector
		);

		return facetCounts.getTopChildren( limit, absoluteFieldPath );
	}

	@Override
	Set<String> collectFirstTerms(IndexReader reader, boolean descending, int limit)
			throws IOException {
		TreeSet<String> collectedTerms = new TreeSet<>( descending ? STRING_COMPARATOR.reversed() : STRING_COMPARATOR );
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
	String labelToTerm(String label) {
		return label;
	}

	@Override
	String termToFieldValue(String key) {
		return key;
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

		@Override
		public <K> Builder<K> type(Class<K> expectedType, ValueConvert convert) {
			return new Builder<>( scope, field,
					field.type().projectionConverter( convert ).withConvertedType( expectedType, field ) );
		}
	}

	private static class Builder<K>
			extends AbstractBuilder<String, String, K> {

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
