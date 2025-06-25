/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.facet.impl;

import java.util.Collection;
import java.util.function.ToLongFunction;

import org.hibernate.search.backend.lucene.types.aggregation.impl.EffectiveRange;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.data.RangeBoundInclusion;

public class FacetCountsUtils {

	private FacetCountsUtils() {
	}

	public static <T extends Number> EffectiveRange[] createEffectiveRangesForIntegralValues(
			Collection<? extends Range<? extends T>> ranges) {
		return createEffectiveRangesForIntegralValues( ranges, Number::longValue, Long.MIN_VALUE, Long.MAX_VALUE, false );
	}

	public static <T extends Number> EffectiveRange[] createEffectiveRangesForIntegralValues(
			Collection<? extends Range<? extends T>> ranges,
			ToLongFunction<T> encoder, T negativeInfinity, T positiveInfinity) {
		return createEffectiveRangesForIntegralValues( ranges, encoder, negativeInfinity, positiveInfinity, true );
	}

	private static <T> EffectiveRange[] createEffectiveRangesForIntegralValues(Collection<? extends Range<? extends T>> ranges,
			ToLongFunction<T> encoder,
			T lowestPossibleValue, T highestPossibleValue, boolean extremaAreInfinity) {
		EffectiveRange[] effectiveRanges = new EffectiveRange[ranges.size()];
		int i = 0;
		for ( Range<? extends T> range : ranges ) {
			final T lowerBoundValue = range.lowerBoundValue().orElse( null );
			final T upperBoundValue = range.upperBoundValue().orElse( null );


			long min = encoder.applyAsLong( lowerBoundValue == null ? lowestPossibleValue : lowerBoundValue );
			long max = encoder.applyAsLong( upperBoundValue == null ? highestPossibleValue : upperBoundValue );

			// The lower bound is included if it is explicitly included
			// ... or if it is infinity but infinity cannot be represented
			// so if it's none of the above we exclude the boundary by ++ it.
			if (
				RangeBoundInclusion.EXCLUDED.equals( range.lowerBoundInclusion() )
						&& ( extremaAreInfinity || lowerBoundValue != null ) ) {
				++min;
			}

			// The upper bound is included if it is explicitly included
			// ... or if it is infinity but infinity cannot be represented
			// so if it's none of the above we exclude the boundary by -- it.
			if (
				RangeBoundInclusion.EXCLUDED.equals( range.upperBoundInclusion() )
						&& ( extremaAreInfinity || upperBoundValue != null ) ) {
				--max;
			}

			effectiveRanges[i] = new EffectiveRange(
					min,
					max
			);
			++i;
		}
		return effectiveRanges;
	}
}
