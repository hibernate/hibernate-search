/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin.time.impl;

import static java.time.temporal.ChronoField.YEAR;

import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;

/**
 * Converts a {@link Year} to a {@link String}.
 * <p>
 * A {@code Year} 2 becomes the string {@code +000000002}.
 * <p>
 * The sign is always present and the values are padded with 0 to allow field sorting.
 *
 * @author Davide D'Alto
 */
public class YearBridge extends TemporalAccessorStringBridge<Year> {

	static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.appendValue( YEAR, 9, 9, SignStyle.ALWAYS )
			.toFormatter();

	public static final YearBridge INSTANCE = new YearBridge();

	private YearBridge() {
		super( FORMATTER, Year.class );
	}

	@Override
	Year parse(String stringValue) throws Exception {
		return Year.parse( stringValue, FORMATTER );
	}
}
