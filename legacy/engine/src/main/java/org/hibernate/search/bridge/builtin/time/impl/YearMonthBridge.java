/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin.time.impl;

import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.SignStyle;

/**
 * Converts a {@link YearMonth} to a {@link String}.
 * <p>
 * A {@code YearMonth} 12345-1 becomes the string {@code +00001234501}.
 * <p>
 * THe signn is alway present and the values are padded with 0 to allow field sorting.
 *
 * @author Davide D'Alto
 */
public class YearMonthBridge extends TemporalAccessorStringBridge<YearMonth> {

	static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.parseCaseInsensitive()
			.parseStrict()
			.appendValue( YEAR, 9, 9, SignStyle.ALWAYS )
			.appendValue( MONTH_OF_YEAR, 2 )
			.toFormatter();

	public static final YearMonthBridge INSTANCE = new YearMonthBridge();

	private YearMonthBridge() {
		super( FORMATTER, YearMonth.class );
	}

	@Override
	YearMonth parse(String stringValue) throws DateTimeParseException {
		return YearMonth.parse( stringValue, FORMATTER );
	}
}
