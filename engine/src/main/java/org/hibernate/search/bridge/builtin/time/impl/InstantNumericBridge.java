/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin.time.impl;

import java.time.Instant;

/**
 * Converts an {@link Instant} to the number of milliseconds from the epoch of 1970-01-01T00:00:00Z.
 * <p>
 * If the instant represents a point on the time-line too far in the future or past to fit in a {@code long}
 * milliseconds, then an exception is thrown.
 * <p>
 * If the instant has greater than millisecond precision, then the conversion will drop any excess precision
 * information as though the amount in nanoseconds was subject to integer division by one million.
 * <p>
 * The biggest instant that can be converted using {@link Long#MAX_VALUE} is 292278994-08-17T07:12:55.807Z.
 * The smallest instant that can be converted using {@link Long#MIN_VALUE} is -292275055-05-16T16:47:04.192Z.
 *
 * @author Davide D'Alto
 */
public class InstantNumericBridge extends JavaTimeNumericBridge<Instant, Long> {

	public static final InstantNumericBridge INSTANCE = new InstantNumericBridge();

	private InstantNumericBridge() {
	}

	/**
	 * @see Instant#toEpochMilli()
	 */
	@Override
	public Long encode(Instant instant) {
		return instant.toEpochMilli();
	}

	/**
	 * @see Instant#ofEpochMilli(long)
	 */
	@Override
	public Instant decode(Long value) {
		return Instant.ofEpochMilli( value );
	}
}
