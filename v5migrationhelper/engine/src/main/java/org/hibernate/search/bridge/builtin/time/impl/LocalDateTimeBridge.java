/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin.time.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

/**
 * Converts a {@link LocalDateTime} to a {@link String}.
 * <p>
 * A {@code LocalDateTime} +2012-12-31T23:59:59.999 becomes the string {@code +0000020121231235950000000999}.
 * <p>
 * The sign is always present for the year and the string is padded with 0 to allow field sorting.
 *
 * @author Davide D'Alto
 */
public class LocalDateTimeBridge extends TemporalAccessorStringBridge<LocalDateTime> {

	static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.append( LocalDateBridge.FORMATTER )
			.append( LocalTimeBridge.FORMATTER )
			.toFormatter();

	public static final LocalDateTimeBridge INSTANCE = new LocalDateTimeBridge();

	private LocalDateTimeBridge() {
		super( FORMATTER, LocalDateTime.class );
	}

	@Override
	LocalDateTime parse(String stringValue) throws DateTimeParseException {
		return LocalDateTime.parse( stringValue, FORMATTER );
	}
}
