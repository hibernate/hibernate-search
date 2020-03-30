/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import org.apache.lucene.search.LongValues;

final class DocValuesUtils {

	private DocValuesUtils() {
	}

	public static final LongValues LONG_VALUES_EMPTY = new LongValues() {
		@Override
		public long longValue() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean advanceExact(int doc) {
			return false;
		}
	};

}
