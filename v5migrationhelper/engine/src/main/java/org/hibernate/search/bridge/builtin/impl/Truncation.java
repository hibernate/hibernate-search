/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.bridge.builtin.impl;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalUnit;

import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.util.common.AssertionFailure;

public class Truncation {
	public static Truncation at(Resolution resolution) {
		switch ( resolution ) {
			case YEAR:
				return YEAR;
			case MONTH:
				return MONTH;
			case DAY:
				return DAY;
			case HOUR:
				return HOUR;
			case MINUTE:
				return MINUTE;
			case SECOND:
				return SECOND;
			case MILLISECOND:
				return MILLISECOND;
			default:
				throw new AssertionFailure( "Unknown Resolution: " + resolution );
		}
	}

	static final Truncation YEAR = new Truncation( TemporalAdjusters.firstDayOfYear(), ChronoUnit.DAYS );
	static final Truncation MONTH = new Truncation( TemporalAdjusters.firstDayOfMonth(), ChronoUnit.DAYS );
	static final Truncation DAY = new Truncation( null, ChronoUnit.DAYS );
	static final Truncation HOUR = new Truncation( null, ChronoUnit.HOURS );
	static final Truncation MINUTE = new Truncation( null, ChronoUnit.MINUTES );
	static final Truncation SECOND = new Truncation( null, ChronoUnit.SECONDS );
	static final Truncation MILLISECOND = new Truncation( null, ChronoUnit.MILLIS );

	private final TemporalAdjuster dateAdjuster;
	private final TemporalUnit timeLowestUnit;

	private Truncation(TemporalAdjuster dateAdjuster, TemporalUnit timeLowestUnit) {
		this.dateAdjuster = dateAdjuster;
		this.timeLowestUnit = timeLowestUnit;
	}

	public ZonedDateTime truncate(ZonedDateTime value) {
		ZonedDateTime result = value;
		if ( dateAdjuster != null ) {
			result = result.with( dateAdjuster );
		}
		result = result.truncatedTo( timeLowestUnit );
		return result;
	}
}
