/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;

import org.apache.lucene.index.NumericDocValues;

abstract class SingletonNumericDoubleDocValues extends SortedNumericDoubleDocValues {

	protected final NumericDocValues values;

	SingletonNumericDoubleDocValues(NumericDocValues values) {
		this.values = values;
	}

	public abstract NumericDoubleValues toNumericDoubleValues();

	@Override
	public final boolean advanceExact(int doc) throws IOException {
		return values.advanceExact( doc );
	}

	@Override
	public final int advance(int target) throws IOException {
		return values.advance( target );
	}

	@Override
	public final int nextDoc() throws IOException {
		return values.nextDoc();
	}

	@Override
	public final int docID() {
		return values.docID();
	}

	@Override
	public final int docValueCount() {
		return 1;
	}

	@Override
	public final long cost() {
		return values.cost();
	}
}
