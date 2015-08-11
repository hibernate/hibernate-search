/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin.time.impl;

import java.time.LocalTime;

/**
 * Converts a {@link LocalTime} to a {@link Long}.
 * <p>
 * The digits of the resulting number will have the following pattern: hhMMssnnnnnnnnn
 * <p>
 * The values during encoding and decoding must be between the boundaries of a {@link LocalTime};
 * if some of the values are out of range, a {@link java.time.DateTimeException} is thrown.
 *
 * @author Davide D'Alto
 */
public class LocalTimeNumericBridge extends JavaTimeNumericBridge<LocalTime, Long> {

	public static final LocalTimeNumericBridge INSTANCE = new LocalTimeNumericBridge();

	private LocalTimeNumericBridge() {
	}

	@Override
	public Long encode(LocalTime time) {
		long hour = (long) time.getHour() * 1_00_00_000_000_000L;
		long min = (long) time.getMinute() * 1_00_000_000_000L;
		long sec = (long) time.getSecond() * 1_000_000_000L;
		long nano = (long) time.getNano();
		return hour + min + sec + nano;
	}

	@Override
	public LocalTime decode(Long value) {
		int hour = (int) ( value / 1_00_00_000_000_000L );
		int min = (int) ( ( value / 1_00_000_000_000L ) % 100L );
		int sec = (int) ( ( value / 1_000_000_000L ) % 100L );
		int nano = (int) ( value % 1_000_000_000L );
		return LocalTime.of( hour, min, sec, nano );
	}
}
