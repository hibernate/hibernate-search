/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin.time.impl;

import java.time.MonthDay;

/**
 * Converts a {@link MonthDay} to an {@link Integer}.
 * <p>
 * For example, the date 2000-11 will become the number 200011.
 *
 * @author Davide D'Alto
 */
public class MonthDayNumericBridge extends JavaTimeNumericBridge<MonthDay, Integer> {

	public static final MonthDayNumericBridge INSTANCE = new MonthDayNumericBridge();

	private MonthDayNumericBridge() {
	}

	@Override
	public Integer encode(MonthDay monthDay) {
		int month = monthDay.getMonthValue() * 100;
		int day = monthDay.getDayOfMonth();
		return month + day;
	}

	/**
	 * Converts an {@link Integer} to a {@link MonthDay}.
	 *
	 * @param value the number representing a {@link MonthDay}
	 * @return a valid {@link MonthDay}
	 *
	 * @see MonthDay#of(java.time.Month, int)
	 * @throws java.time.DateTimeException if the value does not represents a valid {@link MonthDay}
	 */
	@Override
	public MonthDay decode(Integer value) {
		int month = value / 100;
		int day = value % 100;
		return MonthDay.of( month, day );
	}
}
