/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.sort.comparatorsource.impl;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.MultiValueMode;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;

import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.Pruning;
import org.apache.lucene.search.Query;

public class LuceneNumericFieldComparatorSource<E extends Number> extends LuceneFieldComparatorSource {

	private final E missingValue;
	private final LuceneNumericDomain<E> numericDomain;
	private final MultiValueMode sortMode;

	public LuceneNumericFieldComparatorSource(String nestedDocumentPath, LuceneNumericDomain<E> numericDomain, E missingValue,
			MultiValueMode sortMode, Query filter) {
		super( nestedDocumentPath, filter );
		this.numericDomain = numericDomain;
		this.missingValue = missingValue;
		this.sortMode = sortMode;
	}

	@Override
	public FieldComparator<?> newComparator(String fieldname, int numHits, Pruning pruning, boolean reversed) {
		return numericDomain.createFieldComparator( fieldname, numHits, missingValue, reversed, pruning,
				sortMode, nestedDocsProvider );
	}
}
