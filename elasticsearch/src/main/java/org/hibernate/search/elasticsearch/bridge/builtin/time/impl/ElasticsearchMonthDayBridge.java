/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.bridge.builtin.time.impl;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;

import java.time.DateTimeException;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.Locale;

import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Converts a {@link MonthDay} to a {@link String} in Elasticsearch's "--MM-dd" format.
 *
 * @author Yoann Rodiere
 */
public class ElasticsearchMonthDayBridge extends ElasticsearchTemporalAccessorStringBridge<MonthDay> {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.appendLiteral( "--" )
			.appendValue( MONTH_OF_YEAR, 2 )
			.appendLiteral( '-' )
			.appendValue( DAY_OF_MONTH, 2 )
			.toFormatter( Locale.ROOT )
			.withResolverStyle( ResolverStyle.STRICT );

	public static final ElasticsearchMonthDayBridge INSTANCE = new ElasticsearchMonthDayBridge();

	private ElasticsearchMonthDayBridge() {
		super( FORMATTER, MonthDay.class );
	}

	@Override
	MonthDay parse(DateTimeFormatter formatter, String stringValue) throws DateTimeException {
		return MonthDay.parse( stringValue, formatter );
	}

	@Override
	protected IllegalArgumentException createInvalidIndexNullAsException(String indexNullAs, DateTimeException e) {
		return LOG.invalidNullMarkerForMonthDay( e );
	}
}
