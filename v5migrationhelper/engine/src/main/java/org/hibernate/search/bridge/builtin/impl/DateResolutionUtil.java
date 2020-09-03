/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
