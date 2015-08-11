/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin.time.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Converts a {@link LocalDateTime} to number of millisecond from Epoch using a UTC offset.
 * <p>
 * The LocalDateTime is converted to an {@link Instant} using {@link ZoneOffset#UTC}:
 * <p>
 * If the instant represents a point on the time-line too far in the future or past to fit in a {@code long}
 * milliseconds, then an exception is thrown.
 * <p>
 * If the instant has greater than millisecond precision, then the conversion will drop any excess precision information
 * as though the amount in nanoseconds was subject to integer division by one million.
 * <p>
 * The biggest date that can be converted using {@link Long#MAX_VALUE} is 292278994-08-17T07:12:55.807.
 * The smallest date that can be converted using {@link Long#MIN_VALUE} is -292275055-05-16T16:47:04.192.
 *
 * @author Davide D'Alto
 */
public class LocalDateTimeNumericBridge extends JavaTimeNumericBridge<LocalDateTime, Long> {

	public static final LocalDateTimeNumericBridge INSTANCE = new LocalDateTimeNumericBridge();

	private LocalDateTimeNumericBridge() {
	}

	@Override
	public Long encode(LocalDateTime dateTime) {
		return dateTime.atOffset( ZoneOffset.UTC ).toInstant().toEpochMilli();
	}

	@Override
	public LocalDateTime decode(Long value) {
		return Instant.ofEpochMilli( value ).atOffset( ZoneOffset.UTC ).toLocalDateTime();
	}
}
