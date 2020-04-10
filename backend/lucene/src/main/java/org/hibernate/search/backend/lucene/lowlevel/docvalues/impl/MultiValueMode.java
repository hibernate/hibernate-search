/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;
import java.util.OptionalDouble;
import java.util.OptionalLong;

import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.search.DocIdSetIterator;

/**
 * Defines what values to pick in the case a document contains multiple values
 * for a particular field.
 */
public enum MultiValueMode {

	SUM {
		@Override
		OptionalLong pick(SortedNumericDocValues values) throws IOException {
			long result = 0;
			final int valueCount = values.docValueCount();
			boolean hasValue = valueCount > 0;
			for ( int index = 0; index < valueCount; ++index ) {
				result += values.nextValue();
			}
			return hasValue ? OptionalLong.of( result ) : OptionalLong.empty();
		}

		@Override
		OptionalLong pick(SortedNumericDocValues values, DocIdSetIterator docItr, int startDoc, int endDoc) throws IOException {
			long result = 0;
			boolean hasValue = false;
			for ( int doc = startDoc; doc < endDoc; doc = docItr.nextDoc() ) {
				if ( values.advanceExact( doc ) ) {
					final int valueCountForChild = values.docValueCount();
					for ( int index = 0; index < valueCountForChild; ++index ) {
						result += values.nextValue();
						hasValue = true;
					}
				}
			}
			return hasValue ? OptionalLong.of( result ) : OptionalLong.empty();
		}

		@Override
		OptionalDouble pick(SortedNumericDoubleDocValues values) throws IOException {
			final int valueCount = values.docValueCount();
			double result = 0;
			boolean hasValue = valueCount > 0;
			for ( int index = 0; index < valueCount; ++index ) {
				result += values.nextValue();
			}
			return hasValue ? OptionalDouble.of( result ) : OptionalDouble.empty();
		}

		@Override
		OptionalDouble pick(SortedNumericDoubleDocValues values, DocIdSetIterator docItr, int startDoc, int endDoc)
			throws IOException {
			double result = 0;
			boolean hasValue = false;
			for ( int doc = startDoc; doc < endDoc; doc = docItr.nextDoc() ) {
				if ( values.advanceExact( doc ) ) {
					final int valueCountForChild = values.docValueCount();
					for ( int index = 0; index < valueCountForChild; ++index ) {
						result += values.nextValue();
						hasValue = true;
					}
				}
			}
			return hasValue ? OptionalDouble.of( result ) : OptionalDouble.empty();
		}
	},
	AVG {
		@Override
		OptionalLong pick(SortedNumericDocValues values) throws IOException {
			final int valueCount = values.docValueCount();
			long result = 0;
			boolean hasValue = valueCount > 0;
			for ( int index = 0; index < valueCount; ++index ) {
				result += values.nextValue();
				hasValue = true;
			}
			result = result / valueCount;
			return hasValue ? OptionalLong.of( result ) : OptionalLong.empty();
		}

		@Override
		OptionalLong pick(SortedNumericDocValues values, DocIdSetIterator docItr, int startDoc, int endDoc) throws IOException {
			long returnValue = 0;
			int valueCount = 0;
			boolean hasValue = false;
			for ( int doc = startDoc; doc < endDoc; doc = docItr.nextDoc() ) {
				if ( values.advanceExact( doc ) ) {
					final int valueCountForChild = values.docValueCount();
					for ( int index = 0; index < valueCountForChild; ++index ) {
						returnValue += values.nextValue();
						hasValue = true;
					}
					valueCount += valueCountForChild;
				}
			}
			if ( valueCount > 0 ) {
				returnValue = returnValue / valueCount;
			}
			else {
				returnValue = 0;
			}
			return hasValue ? OptionalLong.of( returnValue ) : OptionalLong.empty();
		}

		@Override
		OptionalDouble pick(SortedNumericDoubleDocValues values) throws IOException {
			final int valueCount = values.docValueCount();
			double result = 0;
			boolean hasValue = valueCount > 0;
			for ( int index = 0; index < valueCount; ++index ) {
				result += values.nextValue();
				hasValue = true;
			}
			result = result / valueCount;
			return hasValue ? OptionalDouble.of( result ) : OptionalDouble.empty();
		}

		@Override
		OptionalDouble pick(SortedNumericDoubleDocValues values, DocIdSetIterator docItr, int startDoc, int endDoc)
			throws IOException {
			double result = 0;
			int valueCount = 0;
			boolean hasValue = false;
			for ( int doc = startDoc; doc < endDoc; doc = docItr.nextDoc() ) {
				if ( values.advanceExact( doc ) ) {
					final int valueCountForChild = values.docValueCount();
					for ( int index = 0; index < valueCountForChild; ++index ) {
						result += values.nextValue();
						hasValue = true;
					}
					valueCount += valueCountForChild;
				}
			}
			if ( valueCount > 0 ) {
				result = result / valueCount;
			}
			else {
				result = 0;
			}
			return hasValue ? OptionalDouble.of( result ) : OptionalDouble.empty();
		}
	},
	MIN {
		@Override
		OptionalLong pick(SortedNumericDocValues values) throws IOException {
			boolean hasValue = values.docValueCount() > 0;
			// Values are sorted; the first value is the min.
			return hasValue ? OptionalLong.of( values.nextValue() ) : OptionalLong.empty();
		}

		@Override
		OptionalLong pick(SortedNumericDocValues values, DocIdSetIterator docItr, int startDoc, int endDoc) throws IOException {
			long result = Long.MAX_VALUE;
			boolean hasValue = false;
			for ( int doc = startDoc; doc < endDoc; doc = docItr.nextDoc() ) {
				if ( values.advanceExact( doc ) ) {
					// Values are sorted; the first value is the min for this document.
					hasValue = true;
					result = Math.min( result, values.nextValue() );
				}
			}
			return hasValue ? OptionalLong.of( result ) : OptionalLong.empty();
		}

		@Override
		OptionalDouble pick(SortedNumericDoubleDocValues values) throws IOException {
			boolean hasValue = values.docValueCount() > 0;
			// Values are sorted; the first value is the min.
			return hasValue ? OptionalDouble.of( values.nextValue() ) : OptionalDouble.empty();
		}

		@Override
		OptionalDouble pick(SortedNumericDoubleDocValues values, DocIdSetIterator docItr, int startDoc, int endDoc)
			throws IOException {
			double result = Double.POSITIVE_INFINITY;
			boolean hasValue = false;
			for ( int doc = startDoc; doc < endDoc; doc = docItr.nextDoc() ) {
				if ( values.advanceExact( doc ) ) {
					// Values are sorted; the first value is the min for this document.
					hasValue = true;
					result = Math.min( result, values.nextValue() );
				}
			}
			return hasValue ? OptionalDouble.of( result ) : OptionalDouble.empty();
		}

		@Override
		OptionalLong pick(SortedSetDocValues values) throws IOException {
			long result = Long.MAX_VALUE;
			boolean hasValue = values.getValueCount() > 0;
			for ( long ord; (ord = values.nextOrd()) != SortedSetDocValues.NO_MORE_ORDS; ) {
				result = Math.min( result, ord );
			}
			return hasValue ? OptionalLong.of( result ) : OptionalLong.empty();
		}

		@Override
		OptionalLong pick(SortedSetDocValues values, DocIdSetIterator docItr, int startDoc, int endDoc) throws IOException {
			long result = Long.MAX_VALUE;
			boolean hasValue = false;
			for ( int doc = startDoc; doc < endDoc; doc = docItr.nextDoc() ) {
				if ( values.advanceExact( doc ) ) {
					for ( long ord; (ord = values.nextOrd()) != SortedSetDocValues.NO_MORE_ORDS; ) {
						result = Math.min( result, ord );
						hasValue = true;
					}
				}
			}
			return hasValue ? OptionalLong.of( result ) : OptionalLong.empty();
		}
	},
	MAX {
		@Override
		OptionalLong pick(SortedNumericDocValues values) throws IOException {
			final int valueCount = values.docValueCount();
			boolean hasValue = valueCount > 0;
			// Values are sorted; the last value is the max.
			for ( int index = 0; index < valueCount - 1; ++index ) {
				values.nextValue();
			}
			return hasValue ? OptionalLong.of( values.nextValue() ) : OptionalLong.empty();
		}

		@Override
		OptionalLong pick(SortedNumericDocValues values, DocIdSetIterator docItr, int startDoc, int endDoc) throws IOException {
			long result = Long.MIN_VALUE;
			boolean hasValue = false;
			for ( int doc = startDoc; doc < endDoc; doc = docItr.nextDoc() ) {
				if ( values.advanceExact( doc ) ) {
					final int valueCountForChild = values.docValueCount();
					// Values are sorted; the last value is the max for this document.
					for ( int index = 0; index < valueCountForChild - 1; ++index ) {
						values.nextValue();
					}
					hasValue = true;
					result = Math.max( result, values.nextValue() );
				}
			}
			return hasValue ? OptionalLong.of( result ) : OptionalLong.empty();
		}

		@Override
		OptionalDouble pick(SortedNumericDoubleDocValues values) throws IOException {
			final int valueCount = values.docValueCount();
			boolean hasValue = valueCount > 0;
			// Values are sorted; the last value is the max.
			for ( int index = 0; index < valueCount - 1; ++index ) {
				values.nextValue();
			}
			return hasValue ? OptionalDouble.of( values.nextValue() ) : OptionalDouble.empty();
		}

		@Override
		OptionalDouble pick(SortedNumericDoubleDocValues values, DocIdSetIterator docItr, int startDoc, int endDoc)
			throws IOException {
			double result = Double.NEGATIVE_INFINITY;
			boolean hasValue = false;
			for ( int doc = startDoc; doc < endDoc; doc = docItr.nextDoc() ) {
				if ( values.advanceExact( doc ) ) {
					final int valueCountForChild = values.docValueCount();
					// Values are sorted; the last value is the max for this document.
					for ( int index = 0; index < valueCountForChild - 1; ++index ) {
						values.nextValue();
					}
					hasValue = true;
					result = Math.max( result, values.nextValue() );
				}
			}
			return hasValue ? OptionalDouble.of( result ) : OptionalDouble.empty();
		}

		@Override
		OptionalLong pick(SortedSetDocValues values) throws IOException {
			long result = Long.MIN_VALUE;
			boolean hasValue = values.getValueCount() > 0;
			for ( long ord; (ord = values.nextOrd()) != SortedSetDocValues.NO_MORE_ORDS; ) {
				result = Math.max( result, ord );
			}
			return hasValue ? OptionalLong.of( result ) : OptionalLong.empty();
		}

		@Override
		OptionalLong pick(SortedSetDocValues values, DocIdSetIterator docItr, int startDoc, int endDoc) throws IOException {
			long returnValue = Long.MIN_VALUE;
			boolean hasValue = false;
			for ( int doc = startDoc; doc < endDoc; doc = docItr.nextDoc() ) {
				if ( values.advanceExact( doc ) ) {
					for ( long ord; (ord = values.nextOrd()) != SortedSetDocValues.NO_MORE_ORDS; ) {
						returnValue = Math.max( returnValue, ord );
						hasValue = true;
					}
				}
			}
			return hasValue ? OptionalLong.of( returnValue ) : OptionalLong.empty();
		}
	},
	MEDIAN {
		@Override
		OptionalLong pick(SortedNumericDocValues values) throws IOException {
			final int valueCount = values.docValueCount();
			long result;
			boolean hasValue = valueCount > 0;
			for ( int i = 0; i < (valueCount - 1) / 2; ++i ) {
				values.nextValue();
			}
			if ( valueCount % 2 == 0 ) {
				result = (values.nextValue() + values.nextValue()) / 2;
			}
			else {
				result = values.nextValue();
			}
			return hasValue ? OptionalLong.of( result ) : OptionalLong.empty();
		}

		@Override
		OptionalDouble pick(SortedNumericDoubleDocValues values) throws IOException {
			final int valueCount = values.docValueCount();
			double result;
			boolean hasValue = valueCount > 0;
			for ( int i = 0; i < (valueCount - 1) / 2; ++i ) {
				values.nextValue();
			}
			if ( valueCount % 2 == 0 ) {
				result = (values.nextValue() + values.nextValue()) / 2;
			}
			else {
				result = values.nextValue();
			}
			return hasValue ? OptionalDouble.of( result ) : OptionalDouble.empty();
		}
	},
	NONE {
		@Override
		OptionalLong pick(SortedNumericDocValues values) throws IOException {
			throw new IllegalArgumentException( "Unsupported sort mode: " + this );
		}

		@Override
		OptionalDouble pick(SortedNumericDoubleDocValues values) throws IOException {
			throw new IllegalArgumentException( "Unsupported sort mode: " + this );
		}
	};

	abstract OptionalLong pick(SortedNumericDocValues values) throws IOException;

	OptionalLong pick(SortedNumericDocValues values, DocIdSetIterator docItr, int startDoc, int endDoc) throws IOException {
		throw new IllegalArgumentException( "Unsupported sort mode: " + this );
	}

	abstract OptionalDouble pick(SortedNumericDoubleDocValues values) throws IOException;

	OptionalDouble pick(SortedNumericDoubleDocValues values, DocIdSetIterator docItr, int startDoc, int endDoc) throws IOException {
		throw new IllegalArgumentException( "Unsupported sort mode: " + this );
	}

	OptionalLong pick(SortedSetDocValues values) throws IOException {
		throw new IllegalArgumentException( "Unsupported sort mode: " + this );
	}

	OptionalLong pick(SortedSetDocValues values, DocIdSetIterator docItr, int startDoc, int endDoc) throws IOException {
		throw new IllegalArgumentException( "Unsupported sort mode: " + this );
	}
}
