/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;

import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.util.BytesRef;

/**
 * Handles a replacement for missing sorted document values.
 * <p>
 * Copied with some changes from
 * {@code org.elasticsearch.index.fielddata.fieldcomparator.BytesRefComparatorSource.ReplaceMissing}
 * of <a href="https://github.com/elastic/elasticsearch">Elasticsearch project</a>.
 *
 */
public class ReplaceMissingSortedDocValues extends SortedDocValues {
	final SortedDocValues originalValues;
	final BytesRef missingValue;
	final int missingValuePosition;
	final boolean missingValueExist;

	boolean hasValue = false;

	public ReplaceMissingSortedDocValues(SortedDocValues originalValues, BytesRef missingValue) throws IOException {
		this.originalValues = originalValues;
		this.missingValue = missingValue;

		int sub = originalValues.lookupTerm( missingValue );
		if ( sub < 0 ) {
			missingValuePosition = -sub - 1;
			missingValueExist = false;
		}
		else {
			missingValuePosition = sub;
			missingValueExist = true;
		}
	}

	@Override
	public int ordValue() throws IOException {
		if ( !hasValue ) {
			return missingValuePosition;
		}

		int ordValue = originalValues.ordValue();
		if ( !missingValueExist && ordValue >= missingValuePosition ) {
			ordValue++;
		}

		return ordValue;
	}

	@Override
	public boolean advanceExact(int target) throws IOException {
		hasValue = originalValues.advanceExact( target );
		return true;
	}

	@Override
	public int docID() {
		return originalValues.docID();
	}

	@Override
	public int getValueCount() {
		if ( missingValueExist ) {
			return originalValues.getValueCount();
		}
		return originalValues.getValueCount() + 1;
	}

	@Override
	public BytesRef lookupOrd(int ord) throws IOException {
		if ( ord == missingValuePosition ) {
			return missingValue;
		}
		if ( !missingValueExist && ord > missingValuePosition ) {
			return originalValues.lookupOrd( ord - 1 );
		}
		return originalValues.lookupOrd( ord );
	}

	@Override
	public int nextDoc() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int advance(int target) {
		throw new UnsupportedOperationException();
	}

	@Override
	public long cost() {
		throw new UnsupportedOperationException();
	}
}
