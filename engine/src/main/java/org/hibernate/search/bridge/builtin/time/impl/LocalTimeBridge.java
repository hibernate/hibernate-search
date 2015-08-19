/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin.time.impl;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

/**
 * Converts a {@link LocalTime} to a {@link String}.
 * <p>
 * A {@code LocalDateTime} 23:59:59.999 becomes the string {@code 235950000000999}.
 * <p>
 * The sign is always present for the year and the string is padded with 0 to allow field sorting.
 *
 * @author Davide D'Alto
 */
public class LocalTimeBridge extends TemporalAccessorStringBridge<LocalTime> {

	static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.appendValue( HOUR_OF_DAY, 2 )
			.appendValue( MINUTE_OF_HOUR, 2 )
			.appendValue( SECOND_OF_MINUTE, 2)
			.appendValue( NANO_OF_SECOND, 9 )
			.toFormatter();

	public static final LocalTimeBridge INSTANCE = new LocalTimeBridge();

	private LocalTimeBridge() {
		super( FORMATTER, LocalTime.class );
	}

	@Override
	LocalTime parse(String stringValue) throws DateTimeParseException {
		return LocalTime.parse( stringValue, FORMATTER );
	}
}
