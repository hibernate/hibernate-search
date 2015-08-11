/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin.time.impl;

import java.time.LocalDate;

/**
 * Converts a {@link LocalDate} to a {@link Long}.
 * <p>
 * The digits of the resulting number will have the following pattern: yyyyMMdd
 * <p>
 * For example, the date 2001-3-25 will become the number 20010325.
 * <p>
 * The values during encoding and decoding must be between the boundaries of a {@link LocalDate};
 * if some of the values are out of range, a {@link java.time.DateTimeException} is thrown.
 *
 * @author Davide D'Alto
 */
public class LocalDateNumericBridge extends JavaTimeNumericBridge<LocalDate, Long> {

	public static final LocalDateNumericBridge INSTANCE = new LocalDateNumericBridge();

	private LocalDateNumericBridge() {
	}

	@Override
	public Long encode(LocalDate date) {
		long year = (long) date.getYear() * 10_000L;
		long month = (long) date.getMonthValue() * 100L;
		long day = (long) date.getDayOfMonth();

		return year < 0
				? year - month - day
				: year + month + day;
	}

	@Override
	public LocalDate decode(Long number) {
		long absValue = Math.abs( number );
		int year = (int) ( absValue / 10_000L );
		int month = (int) ( ( absValue / 100L ) % 100L );
		int day = (int) ( absValue % 100 );

		return number.longValue() < 0
				? LocalDate.of( -year, month, day )
				: LocalDate.of( year, month, day );
	}
}
