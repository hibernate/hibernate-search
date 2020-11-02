/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.join.impl.JoinChildrenIdIterator;

import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.index.SortedSetDocValues;

/**
 * Defines what values to pick in the case a document contains multiple values
 * for a particular field.
 */
public enum MultiValueMode {

	SUM {
		@Override
		long pick(SortedNumericDocValues values) throws IOException {
			final int valueCount = values.docValueCount();
			long result = 0;
			for ( int index = 0; index < valueCount; ++index ) {
				result += values.nextValue();
			}
			return result;
		}

		@Override
		long pick(SortedNumericDocValues values, JoinChildrenIdIterator joinIterator) throws IOException {
			long result = 0;
			while ( joinIterator.advanceValuesToNextChild() ) {
				final int valueCountForChild = values.docValueCount();
				for ( int index = 0; index < valueCountForChild; ++index ) {
					result += values.nextValue();
				}
			}
			return result;
		}

		@Override
		double pick(SortedNumericDoubleDocValues values) throws IOException {
			final int valueCount = values.docValueCount();
			double result = 0;
			for ( int index = 0; index < valueCount; ++index ) {
				result += values.nextValue();
			}
			return result;
		}

		@Override
		double pick(SortedNumericDoubleDocValues values, JoinChildrenIdIterator joinIterator)
				throws IOException {
			double result = 0;
			while ( joinIterator.advanceValuesToNextChild() ) {
				final int valueCountForChild = values.docValueCount();
				for ( int index = 0; index < valueCountForChild; ++index ) {
					result += values.nextValue();
				}
			}
			return result;
		}
	},
	AVG {
		@Override
		long pick(SortedNumericDocValues values) throws IOException {
			final int valueCount = values.docValueCount();
			long result = 0;
			for ( int index = 0; index < valueCount; ++index ) {
				result += values.nextValue();
			}
			result = result / valueCount;
			return result;
		}

		@Override
		long pick(SortedNumericDocValues values, JoinChildrenIdIterator joinIterator) throws IOException {
			long returnValue = 0;
			int valueCount = 0;
			while ( joinIterator.advanceValuesToNextChild() ) {
				final int valueCountForChild = values.docValueCount();
				for ( int index = 0; index < valueCountForChild; ++index ) {
					returnValue += values.nextValue();
				}
				valueCount += valueCountForChild;
			}
			if ( valueCount > 0 ) {
				returnValue = returnValue / valueCount;
			}
			else {
				returnValue = 0;
			}
			return returnValue;
		}

		@Override
		double pick(SortedNumericDoubleDocValues values) throws IOException {
			final int valueCount = values.docValueCount();
			double result = 0;
			for ( int index = 0; index < valueCount; ++index ) {
				result += values.nextValue();
			}
			result = result / valueCount;
			return result;
		}

		@Override
		double pick(SortedNumericDoubleDocValues values, JoinChildrenIdIterator joinIterator)
				throws IOException {
			double result = 0;
			int valueCount = 0;
			while ( joinIterator.advanceValuesToNextChild() ) {
				final int valueCountForChild = values.docValueCount();
				for ( int index = 0; index < valueCountForChild; ++index ) {
					result += values.nextValue();
				}
				valueCount += valueCountForChild;
			}
			if ( valueCount > 0 ) {
				result = result / valueCount;
			}
			else {
				result = 0;
			}
			return result;
		}
	},
	MIN {
		@Override
		long pick(SortedNumericDocValues values) throws IOException {
			// Values are sorted; the first value is the min.
			return values.nextValue();
		}

		@Override
		long pick(SortedNumericDocValues values, JoinChildrenIdIterator joinIterator) throws IOException {
			long result = Long.MAX_VALUE;
			while ( joinIterator.advanceValuesToNextChild() ) {
				// Values are sorted; the first value is the min for this document.
				result = Math.min( result, values.nextValue() );
			}
			return result;
		}

		@Override
		double pick(SortedNumericDoubleDocValues values) throws IOException {
			// Values are sorted; the first value is the min.
			return values.nextValue();
		}

		@Override
		double pick(SortedNumericDoubleDocValues values, JoinChildrenIdIterator joinIterator)
				throws IOException {
			double result = Double.POSITIVE_INFINITY;
			while ( joinIterator.advanceValuesToNextChild() ) {
				// Values are sorted; the first value is the min for this document.
				result = Math.min( result, values.nextValue() );
			}
			return result;
		}

		@Override
		long pick(SortedSetDocValues values) throws IOException {
			long result = Long.MAX_VALUE;
			for ( long ord; ( ord = values.nextOrd() ) != SortedSetDocValues.NO_MORE_ORDS; ) {
				result = Math.min( result, ord );
			}
			return result;
		}

		@Override
		long pick(SortedSetDocValues values, JoinChildrenIdIterator joinIterator) throws IOException {
			long result = Long.MAX_VALUE;
			while ( joinIterator.advanceValuesToNextChild() ) {
				for ( long ord; ( ord = values.nextOrd() ) != SortedSetDocValues.NO_MORE_ORDS; ) {
					result = Math.min( result, ord );
				}
			}
			return result;
		}
	},
	MAX {
		@Override
		long pick(SortedNumericDocValues values) throws IOException {
			final int valueCount = values.docValueCount();
			// Values are sorted; the last value is the max.
			for ( int index = 0; index < valueCount - 1; ++index ) {
				values.nextValue();
			}
			return values.nextValue();
		}

		@Override
		long pick(SortedNumericDocValues values, JoinChildrenIdIterator joinIterator) throws IOException {
			long result = Long.MIN_VALUE;
			while ( joinIterator.advanceValuesToNextChild() ) {
				final int valueCountForChild = values.docValueCount();
				// Values are sorted; the last value is the max for this document.
				for ( int index = 0; index < valueCountForChild - 1; ++index ) {
					values.nextValue();
				}
				result = Math.max( result, values.nextValue() );
			}
			return result;
		}

		@Override
		double pick(SortedNumericDoubleDocValues values) throws IOException {
			final int valueCount = values.docValueCount();
			// Values are sorted; the last value is the max.
			for ( int index = 0; index < valueCount - 1; ++index ) {
				values.nextValue();
			}
			return values.nextValue();
		}

		@Override
		double pick(SortedNumericDoubleDocValues values, JoinChildrenIdIterator joinIterator)
				throws IOException {
			double result = Double.NEGATIVE_INFINITY;
			while ( joinIterator.advanceValuesToNextChild() ) {
				final int valueCountForChild = values.docValueCount();
				// Values are sorted; the last value is the max for this document.
				for ( int index = 0; index < valueCountForChild - 1; ++index ) {
					values.nextValue();
				}
				result = Math.max( result, values.nextValue() );
			}
			return result;
		}

		@Override
		long pick(SortedSetDocValues values) throws IOException {
			long result = Long.MIN_VALUE;
			for ( long ord; ( ord = values.nextOrd() ) != SortedSetDocValues.NO_MORE_ORDS; ) {
				result = Math.max( result, ord );
			}
			return result;
		}

		@Override
		long pick(SortedSetDocValues values, JoinChildrenIdIterator joinIterator) throws IOException {
			long returnValue = Long.MIN_VALUE;
			while ( joinIterator.advanceValuesToNextChild() ) {
				for ( long ord; ( ord = values.nextOrd() ) != SortedSetDocValues.NO_MORE_ORDS; ) {
					returnValue = Math.max( returnValue, ord );
				}
			}
			return returnValue;
		}
	},
	MEDIAN {
		@Override
		long pick(SortedNumericDocValues values) throws IOException {
			final int valueCount = values.docValueCount();
			long result = 0;
			for ( int i = 0; i < ( valueCount - 1 ) / 2; ++i ) {
				values.nextValue();
			}
			if ( valueCount % 2 == 0 ) {
				result = ( values.nextValue() + values.nextValue() ) / 2;
			}
			else {
				result = values.nextValue();
			}
			return result;
		}

		@Override
		double pick(SortedNumericDoubleDocValues values) throws IOException {
			final int valueCount = values.docValueCount();
			double result = 0;
			for ( int i = 0; i < ( valueCount - 1 ) / 2; ++i ) {
				values.nextValue();
			}
			if ( valueCount % 2 == 0 ) {
				result = ( values.nextValue() + values.nextValue() ) / 2;
			}
			else {
				result = values.nextValue();
			}
			return result;
		}
	};

	abstract long pick(SortedNumericDocValues values) throws IOException;

	long pick(SortedNumericDocValues values, JoinChildrenIdIterator joinIterator) throws IOException {
		throw unsupportedSortMode();
	}

	abstract double pick(SortedNumericDoubleDocValues values) throws IOException;

	double pick(SortedNumericDoubleDocValues values, JoinChildrenIdIterator joinIterator) throws IOException {
		throw unsupportedSortMode();
	}

	long pick(SortedSetDocValues values) throws IOException {
		throw unsupportedSortMode();
	}

	long pick(SortedSetDocValues values, JoinChildrenIdIterator joinIterator) throws IOException {
		throw unsupportedSortMode();
	}

	IllegalArgumentException unsupportedSortMode() {
		return new IllegalArgumentException( "Unsupported sort mode: " + this );
	}
}
