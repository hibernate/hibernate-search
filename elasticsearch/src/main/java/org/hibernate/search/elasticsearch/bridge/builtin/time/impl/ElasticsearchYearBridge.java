/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.bridge.builtin.time.impl;

import static java.time.temporal.ChronoField.YEAR;

import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.util.Locale;

/**
 * Converts a {@link Year} to a {@link String} in ISO-8601 extended format (9 digits for the year instead of 4).
 *
 * <p>Be aware that this format is <strong>not</strong> the same as Elasticsearch's "strict_year" format,
 * since years with more than 4 digits are allowed.
 *
 * @author Yoann Rodiere
 */
public class ElasticsearchYearBridge extends ElasticsearchTemporalAccessorStringBridge<Year> {

	static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.appendValue( YEAR, 4, 9, SignStyle.EXCEEDS_PAD )
			.toFormatter( Locale.ROOT )
			.withResolverStyle( ResolverStyle.STRICT );

	public static final ElasticsearchYearBridge INSTANCE = new ElasticsearchYearBridge();

	private ElasticsearchYearBridge() {
		super( FORMATTER, Year.class );
	}

	@Override
	Year parse(DateTimeFormatter formatter, String stringValue) throws DateTimeParseException {
		return Year.parse( stringValue, formatter );
	}
}

