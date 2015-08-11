/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin.time.impl;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Converts an {@link Duration} to the number of milliseconds from the epoch of 1970-01-01T00:00:00Z.
 * <p>
 * If the duration is too large to fit in a {@code long} milliseconds, then an
 * exception is thrown.
 * <p>
 * If the duration has greater than millisecond precision, then the conversion will drop any excess precision information
 * as though the amount in nanoseconds was subject to integer division by one million.
 * <p>
 * The biggest instant that can be converted using {@link Long#MAX_VALUE} is w * The biggest instant that can be converted using {@link Long#MAX_VALUE} is 292278994-08-17T07:12:55.807Z.
 * <p>
 * The biggest duration that can be converted using {@link Long#MAX_VALUE} is PT2562047788015H12M55.807S.
 * The smallest duration that can be converted using {@link Long#MIN_VALUE} is PT-2562047788015H-12M-55.808S.
 *
 * @author Davide D'Alto
 */
public class DurationNumericBridge extends JavaTimeNumericBridge<Duration, Long> {

	public static final DurationNumericBridge INSTANCE = new DurationNumericBridge();

	private DurationNumericBridge() {
	}

	/**
	 * @see Duration#toMillis()
	 */
	@Override
	public Long encode(Duration accessor) {
		return accessor.toMillis();
	}

	/**
	 * @see Duration#of(long, java.time.temporal.TemporalUnit)
	 */
	@Override
	public Duration decode(Long value) {
		return Duration.of( value, ChronoUnit.MILLIS );
	}
}
