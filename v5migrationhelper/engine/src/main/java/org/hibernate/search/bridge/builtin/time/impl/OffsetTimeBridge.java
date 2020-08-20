/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin.time.impl;

import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

/**
 * Converts a {@link OffsetTime} to a {@link String}.
 * <p>
 * A {@code OffsetTime} 3:45.30.789+02:00 Europe/Paris becomes the string
 * {@code 034530000000789+02:00}.
 * <p>
 * The sign is always present for the year and the string is padded with 0 to allow field sorting.
 *
 * @author Davide D'Alto
 */
public class OffsetTimeBridge extends TemporalAccessorStringBridge<OffsetTime> {

	private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.append( LocalTimeBridge.FORMATTER )
			.appendOffsetId()
			.toFormatter();

	public static final OffsetTimeBridge INSTANCE = new OffsetTimeBridge();

	private OffsetTimeBridge() {
		super( FORMATTER, OffsetTime.class );
	}

	@Override
	OffsetTime parse(String stringValue) throws DateTimeParseException {
		return OffsetTime.parse( stringValue, FORMATTER );
	}
}
