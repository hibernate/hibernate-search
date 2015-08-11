/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin.time.impl;

import java.time.YearMonth;

/**
 * Converts a {@link YearMonth} to a {@link Long}.
 * <p>
 * The digits of the resulting number will have the following pattern: yyyyMM
 * <p>
 * For example, the date 2001-3 will become the number 200103.
 *
 * @author Davide D'Alto
 */
public class YearMonthNumericBridge extends JavaTimeNumericBridge<YearMonth, Long> {

	public static final YearMonthNumericBridge INSTANCE = new YearMonthNumericBridge();

	private YearMonthNumericBridge() {
	}

	@Override
	public Long encode(YearMonth yearMonth) {
		long year = (long) yearMonth.getYear() * 100L;
		long month = (long) yearMonth.getMonthValue();

		return year < 0
				? year - month
				: year + month;
	}

	/**
	 * Converts a {@link Long} instance to {@link YearMonth} instance.
	 *
	 * @see YearMonth#of(int, int)
	 * @param value an instance of {@link YearMonth}. It must not be null.
	 * @return an {@link YearMonth} instance representing the number
	 * @throws java.time.DateTimeException if the number does not result in a valid {@link YearMonth}
	 */
	@Override
	public YearMonth decode(Long value) {
		long abs = Math.abs( value );
		int year = (int) ( abs / 100L );
		int month = (int) ( abs % 100L );
		return value < 0
				? YearMonth.of( -year, month )
				: YearMonth.of( year, month );
	}
}