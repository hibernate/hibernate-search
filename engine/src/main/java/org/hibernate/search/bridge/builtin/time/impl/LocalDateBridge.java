/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin.time.impl;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.SignStyle;

/**
 * Converts a {@link LocalDate} to a {@link String}.
 * <p>
 * A {@code LocalDate} 2012-12-31 becomes the string {@code +0000020121231}.
 * <p>
 * The sign is always present for the year and the string is padded with 0 to allow field sorting.
 *
 * @author Davide D'Alto
 */
public class LocalDateBridge extends TemporalAccessorStringBridge<LocalDate> {

	static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.appendValue( YEAR, 9, 9, SignStyle.ALWAYS )
			.appendValue( MONTH_OF_YEAR, 2 )
			.appendValue( DAY_OF_MONTH, 2)
			.toFormatter();

	public static final LocalDateBridge INSTANCE = new LocalDateBridge();

	private LocalDateBridge() {
		super( FORMATTER, LocalDate.class );
	}

	@Override
	LocalDate parse(String stringValue) throws DateTimeParseException {
		return LocalDate.parse( stringValue, FORMATTER );
	}
}
