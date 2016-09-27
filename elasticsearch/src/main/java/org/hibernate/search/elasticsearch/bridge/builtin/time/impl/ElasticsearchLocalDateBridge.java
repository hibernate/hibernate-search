/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.bridge.builtin.time.impl;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

/**
 * Converts a {@link LocalDate} to a {@link String} in Elasticsearch's "strict_date" format.
 *
 * <p>Be aware that this format is <strong>not</strong> the same as Elasticsearch's
 * "strict_date" format (since years with more than 4 digits are allowed).
 *
 * @author Yoann Rodiere
 */
public class ElasticsearchLocalDateBridge extends ElasticsearchTemporalAccessorStringBridge<LocalDate> {

	static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.append( ElasticsearchYearMonthBridge.FORMATTER )
			.appendLiteral( '-' )
			.appendValue( DAY_OF_MONTH, 2 )
			.toFormatter( Locale.ROOT )
			.withResolverStyle( ResolverStyle.STRICT );

	public static final ElasticsearchLocalDateBridge INSTANCE = new ElasticsearchLocalDateBridge();

	private ElasticsearchLocalDateBridge() {
		super( FORMATTER, LocalDate.class );
	}

	@Override
	LocalDate parse(DateTimeFormatter formatter, String stringValue) throws DateTimeParseException {
		return LocalDate.parse( stringValue, formatter );
	}
}
