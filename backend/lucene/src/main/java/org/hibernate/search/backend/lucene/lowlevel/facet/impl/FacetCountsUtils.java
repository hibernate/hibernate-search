/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.facet.impl;

import java.util.Collection;
import java.util.function.ToLongFunction;

import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.data.RangeBoundInclusion;

import org.apache.lucene.facet.range.LongRange;

public class FacetCountsUtils {

	private FacetCountsUtils() {
	}

	public static <
			T extends Number> LongRange[] createLongRangesForIntegralValues(Collection<? extends Range<? extends T>> ranges) {
		return createLongRanges( ranges, Number::longValue, Long.MIN_VALUE, Long.MAX_VALUE, false );
	}

	public static <T> LongRange[] createLongRangesForFloatingPointValues(Collection<? extends Range<? extends T>> ranges,
			ToLongFunction<T> encoder, T negativeInfinity, T positiveInfinity) {
		return createLongRanges( ranges, encoder, negativeInfinity, positiveInfinity, true );
	}

	private static <T> LongRange[] createLongRanges(Collection<? extends Range<? extends T>> ranges,
			ToLongFunction<T> encoder,
			T lowestPossibleValue, T highestPossibleValue, boolean extremaAreInfinity) {
		LongRange[] longRanges = new LongRange[ranges.size()];
		int i = 0;
		for ( Range<? extends T> range : ranges ) {
			T lowerBoundValue = range.lowerBoundValue().orElse( null );
			T upperBoundValue = range.upperBoundValue().orElse( null );
			longRanges[i] = new LongRange(
					String.valueOf( i ),
					encoder.applyAsLong( lowerBoundValue == null ? lowestPossibleValue : lowerBoundValue ),
					// The lower bound is included if it is explicitly included
					RangeBoundInclusion.INCLUDED.equals( range.lowerBoundInclusion() )
							// ... or if it is infinity but infinity cannot be represented
							|| !extremaAreInfinity && lowerBoundValue == null,
					encoder.applyAsLong( upperBoundValue == null ? highestPossibleValue : upperBoundValue ),
					// The upper bound is included if it is explicitly included
					RangeBoundInclusion.INCLUDED.equals( range.upperBoundInclusion() )
							// ... or if it is infinity but infinity cannot be represented
							|| !extremaAreInfinity && upperBoundValue == null
			);
			++i;
		}
		return longRanges;
	}

}
