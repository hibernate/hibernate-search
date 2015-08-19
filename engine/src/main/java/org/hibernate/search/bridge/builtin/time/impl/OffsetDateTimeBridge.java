/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin.time.impl;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

/**
 * Converts a {@link OffsetDateTime} to a {@link String}.
 * <p>
 * A {@code MonthDay} 2-31 becomes the string {@code 0231} (pattern: MMdd).
 * <p>
 * The values are padded with 0 to allow field sorting.
 *
 * @author Davide D'Alto
 */
public class OffsetDateTimeBridge extends TemporalAccessorStringBridge<OffsetDateTime> {

	static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.append( LocalDateTimeBridge.FORMATTER )
			.appendOffsetId()
			.toFormatter();

	public static final OffsetDateTimeBridge INSTANCE = new OffsetDateTimeBridge();

	private OffsetDateTimeBridge() {
		super( FORMATTER, OffsetDateTime.class );
	}

	@Override
	OffsetDateTime parse(String stringValue) throws DateTimeParseException {
		return OffsetDateTime.parse( stringValue, FORMATTER );
	}
}
