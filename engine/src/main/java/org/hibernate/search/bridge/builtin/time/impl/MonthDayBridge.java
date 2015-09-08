/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin.time.impl;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;

import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.SignStyle;

/**
 * Converts a {@link MonthDay} to a {@link String}.
 * <p>
 * A {@code MonthDay} 2-31 becomes the string {@code 0231} (pattern: MMdd).
 * <p>
 * The values are padded with 0 to allow field sorting.
 *
 * @author Davide D'Alto
 */
public class MonthDayBridge extends TemporalAccessorStringBridge<MonthDay> {

	private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.appendValue( MONTH_OF_YEAR, 2, 2, SignStyle.NEVER )
			.appendValue( DAY_OF_MONTH, 2, 2, SignStyle.NEVER )
			.toFormatter();

	public static final MonthDayBridge INSTANCE = new MonthDayBridge();

	private MonthDayBridge() {
		super( FORMATTER, MonthDay.class );
	}

	@Override
	MonthDay parse(String stringValue) throws DateTimeParseException {
		return MonthDay.parse( stringValue, FORMATTER );
	}
}
