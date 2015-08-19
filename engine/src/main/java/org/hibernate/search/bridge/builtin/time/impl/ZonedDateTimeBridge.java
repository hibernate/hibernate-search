/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin.time.impl;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

/**
 * Converts a {@link ZonedDateTime} to a {@link String}.
 * <p>
 * A {@code ZonedDateTime} 2012-12-31T23:59:59.999 Europe/Paris becomes the string
 * {@code +0000020121231235959000000999Europe/Paris}.
 * <p>
 * The sign is always present for the year and the string is padded with 0 to allow field sorting.
 *
 * @author Davide D'Alto
 */
public class ZonedDateTimeBridge extends TemporalAccessorStringBridge<ZonedDateTime> {

	static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.append( LocalDateTimeBridge.FORMATTER )
			.appendZoneId()
			.toFormatter();

	public static final ZonedDateTimeBridge INSTANCE = new ZonedDateTimeBridge();

	private ZonedDateTimeBridge() {
		super( FORMATTER, ZonedDateTime.class );
	}

	@Override
	ZonedDateTime parse(String stringValue) throws DateTimeParseException {
		return ZonedDateTime.parse( stringValue, FORMATTER );
	}
}
