/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin.time.impl;

import static java.time.temporal.ChronoField.INSTANT_SECONDS;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.SignStyle;

/**
 * Converts a {@link Instant} to a {@link String}.
 * <p>
 * The string is obtained concatenating the number of seconds from Epoch with the nano of seconds.
 * The values are padded with 0 to allow field sorting.
 *
 * @author Davide D'Alto
 */
public class InstantBridge extends TemporalAccessorStringBridge<Instant> {

	private static final int SECONDS_PADDING = 17;

	private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.appendValue( INSTANT_SECONDS, SECONDS_PADDING, SECONDS_PADDING, SignStyle.ALWAYS )
			.appendValue( NANO_OF_SECOND, 9 )
			.toFormatter();

	public static final InstantBridge INSTANCE = new InstantBridge();

	private InstantBridge() {
		super( FORMATTER, Instant.class );
	}

	@Override
	Instant parse(String stringValue) throws DateTimeParseException {
		long seconds = Long.parseLong( stringValue.substring( 0, SECONDS_PADDING + 1 ) );
		long nanos = Integer.parseInt( stringValue.substring( SECONDS_PADDING + 1 ) );
		return Instant.ofEpochSecond( seconds, nanos );
	}
}
