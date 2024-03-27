/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.lowlevel.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.MultiValueMode;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.util.common.data.Range;

import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.Pruning;
import org.apache.lucene.search.Query;

public interface LuceneNumericDomain<E extends Number> {

	E getMinValue();

	E getMaxValue();

	E getPreviousValue(E value);

	E getNextValue(E value);

	Comparator<E> createComparator();

	Query createExactQuery(String absoluteFieldPath, E value);

	Query createRangeQuery(String absoluteFieldPath, E lowerLimit, E upperLimit);

	Query createSetQuery(String absoluteFieldPath, Collection<E> values);

	E sortedDocValueToTerm(long longValue);

	Facets createTermsFacetCounts(String absoluteFieldPath, FacetsCollector facetsCollector,
			NestedDocsProvider nestedDocsProvider)
			throws IOException;

	Facets createRangeFacetCounts(String absoluteFieldPath,
			FacetsCollector facetsCollector, Collection<? extends Range<? extends E>> ranges,
			NestedDocsProvider nestedDocsProvider)
			throws IOException;

	IndexableField createIndexField(String absoluteFieldPath, E numericValue);

	IndexableField createSortedDocValuesField(String absoluteFieldPath, E numericValue);

	FieldComparator<E> createFieldComparator(String absoluteFieldPath, int numHits,
			E missingValue, boolean reversed, Pruning pruning, MultiValueMode multiValueMode,
			NestedDocsProvider nestedDocsProvider);
}
