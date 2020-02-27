/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.DocIdSetIterator;

import java.io.IOException;

/**
 * Base implementation that throws an {@link IOException} for the
 * {@link DocIdSetIterator} APIs. This impl is safe to use for sorting and
 * aggregations, which only use {@link #advanceExact(int)} and
 * {@link #longValue()}.
 */
public abstract class AbstractNumericDocValues extends NumericDocValues {

	@Override
	public int nextDoc() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int advance(int target) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public long cost() {
		throw new UnsupportedOperationException();
	}

}
