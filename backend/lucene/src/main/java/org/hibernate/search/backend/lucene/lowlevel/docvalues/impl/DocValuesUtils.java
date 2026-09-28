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
