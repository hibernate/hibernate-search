/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.lowlevel.impl;

import java.io.IOException;

import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.util.NumericUtils;

/**
 * Wraps a NumericDocValues and exposes a single 32-bit float per document.
 * <p>
 * Copied and adapted from {@code org.elasticsearch.index.fielddata.plain.SortedNumericDVIndexFieldData.SingleFloatValues} class
 * of <a href="https://github.com/elastic/elasticsearch">Elasticsearch project</a>.
 */
class SingleFloatValues extends NumericDoubleValues {
	final NumericDocValues in;

	SingleFloatValues(NumericDocValues in) {
		this.in = in;
	}

	@Override
	public double doubleValue() throws IOException {
		return NumericUtils.sortableIntToFloat( (int) in.longValue() );
	}

	@Override
	public boolean advanceExact(int doc) throws IOException {
		return in.advanceExact( doc );
	}
}
