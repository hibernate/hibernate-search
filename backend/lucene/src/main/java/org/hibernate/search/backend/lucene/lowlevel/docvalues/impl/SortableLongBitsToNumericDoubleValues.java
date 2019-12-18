/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;

import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.util.NumericUtils;

/**
 * {@link NumericDoubleValues} instance that wraps a {@link NumericDocValues}
 * and converts the doubles to sortable long bits using
 * {@link NumericUtils#sortableLongToDouble(long)}.
 * <p>
 * Copied and adapted from {@code org.elasticsearch.index.fielddata.SortableLongBitsToNumericDoubleValues} class
 * of <a href="https://github.com/elastic/elasticsearch">Elasticsearch project</a>.
 */
final class SortableLongBitsToNumericDoubleValues extends NumericDoubleValues {

	private final NumericDocValues values;

	SortableLongBitsToNumericDoubleValues(NumericDocValues values) {
		this.values = values;
	}

	@Override
	public double doubleValue() throws IOException {
		return NumericUtils.sortableLongToDouble( values.longValue() );
	}

	@Override
	public boolean advanceExact(int doc) throws IOException {
		return values.advanceExact( doc );
	}

	/**
	 * Return the wrapped values.
	 */
	public NumericDocValues getLongValues() {
		return values;
	}

}
