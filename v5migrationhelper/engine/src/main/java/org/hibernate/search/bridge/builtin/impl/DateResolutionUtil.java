/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.bridge.builtin.impl;

import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.util.common.AssertionFailure;

public class DateResolutionUtil {

	private DateResolutionUtil() {
	}

	public static TemporalUnit getLowestTemporalUnit(Resolution resolution) {
		switch ( resolution ) {
			case YEAR:
				return ChronoUnit.YEARS;
			case MONTH:
				return ChronoUnit.MONTHS;
			case DAY:
				return ChronoUnit.DAYS;
			case HOUR:
				return ChronoUnit.HOURS;
			case MINUTE:
				return ChronoUnit.MINUTES;
			case SECOND:
				return ChronoUnit.SECONDS;
			case MILLISECOND:
				return ChronoUnit.MILLIS;
			default:
				throw new AssertionFailure( "Unknown Resolution: " + resolution );
		}
	}
}
