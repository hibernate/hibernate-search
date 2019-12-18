/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;

/**
 * Exposes multi-valued view over a single-valued instance.
 * <p>
 * This can be used if you want to have one multi-valued implementation
 * that works for single or multi-valued types.
 * <p>
 * Copied and adapted from {@code org.elasticsearch.index.fielddata.SingletonSortedNumericDoubleValues} class
 * of <a href="https://github.com/elastic/elasticsearch">Elasticsearch project</a>.
 */
final class SingletonSortedNumericDoubleValues extends SortedNumericDoubleValues {
	private final NumericDoubleValues in;

	SingletonSortedNumericDoubleValues(NumericDoubleValues in) {
		this.in = in;
	}

	/**
	 * Return the wrapped {@link NumericDoubleValues}
	 */
	public NumericDoubleValues getNumericDoubleValues() {
		return in;
	}

	@Override
	public boolean advanceExact(int target) throws IOException {
		return in.advanceExact( target );
	}

	@Override
	public int docValueCount() {
		return 1;
	}

	@Override
	public double nextValue() throws IOException {
		return in.doubleValue();
	}

}
