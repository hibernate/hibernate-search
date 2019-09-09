/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.lowlevel.impl;

import java.util.Collection;

import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.data.RangeBoundInclusion;

import org.apache.lucene.facet.range.DoubleRange;
import org.apache.lucene.facet.range.LongRange;

class FacetCountsUtils {

	private FacetCountsUtils() {
	}

	static LongRange[] createLongRanges(Collection<? extends Range<? extends Number>> ranges) {
		LongRange[] longRanges = new LongRange[ranges.size()];
		int i = 0;
		for ( Range<? extends Number> range : ranges ) {
			Number lowerBoundValue = range.getLowerBoundValue().orElse( null );
			Number upperBoundValue = range.getUpperBoundValue().orElse( null );
			longRanges[i] = new LongRange(
					String.valueOf( i ),
					lowerBoundValue == null ? Long.MIN_VALUE : lowerBoundValue.longValue(),
					// null means -Infinity: if -Infinity is the lower bound, included or not, then Long.MIN_VALUE is included.
					lowerBoundValue == null
							|| RangeBoundInclusion.INCLUDED.equals( range.getLowerBoundInclusion() ),
					upperBoundValue == null ? Long.MAX_VALUE : upperBoundValue.longValue(),
					// null means +Infinity: if +Infinity is the lower bound, included or not, then Long.MAX_VALUE is included.
					upperBoundValue == null
							|| RangeBoundInclusion.INCLUDED.equals( range.getUpperBoundInclusion() )
			);
			++i;
		}
		return longRanges;
	}

	static DoubleRange[] createDoubleRanges(Collection<? extends Range<? extends Number>> ranges) {
		DoubleRange[] doubleRanges = new DoubleRange[ranges.size()];
		int i = 0;
		for ( Range<? extends Number> range : ranges ) {
			Number lowerBoundValue = range.getLowerBoundValue().orElse( null );
			Number upperBoundValue = range.getUpperBoundValue().orElse( null );
			doubleRanges[i] = new DoubleRange(
					String.valueOf( i ),
					lowerBoundValue == null ? Double.NEGATIVE_INFINITY : lowerBoundValue.doubleValue(),
					RangeBoundInclusion.INCLUDED.equals( range.getLowerBoundInclusion() ),
					upperBoundValue == null ? Double.POSITIVE_INFINITY : upperBoundValue.doubleValue(),
					RangeBoundInclusion.INCLUDED.equals( range.getUpperBoundInclusion() )
			);
			++i;
		}
		return doubleRanges;
	}

}
