/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.bridge.builtin.time.impl;

import static java.time.temporal.ChronoField.MONTH_OF_YEAR;

import java.time.DateTimeException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.Locale;

import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Converts a {@link YearMonth} to a {@link String} in ISO-8601 extended format (9 digits for the year instead of 4).
 *
 * <p>Be aware that this format is <strong>not</strong> the same as Elasticsearch's "strict_year_month" format,
 * since years with more than 4 digits are allowed.
 *
 * @author Yoann Rodiere
 */
public class ElasticsearchYearMonthBridge extends ElasticsearchTemporalAccessorStringBridge<YearMonth> {

	private static final Log LOG = LoggerFactory.make( Log.class );

	static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.append( ElasticsearchYearBridge.FORMATTER )
			.appendLiteral( '-' )
			.appendValue( MONTH_OF_YEAR, 2 )
			.toFormatter( Locale.ROOT )
			.withResolverStyle( ResolverStyle.STRICT );

	public static final ElasticsearchYearMonthBridge INSTANCE = new ElasticsearchYearMonthBridge();

	private ElasticsearchYearMonthBridge() {
		super( FORMATTER, YearMonth.class );
	}

	@Override
	YearMonth parse(DateTimeFormatter formatter, String stringValue) throws DateTimeException {
		return YearMonth.parse( stringValue, formatter );
	}

	@Override
	protected IllegalArgumentException createInvalidIndexNullAsException(String indexNullAs, DateTimeException e) {
		return LOG.invalidNullMarkerForYearMonth( e );
	}
}
