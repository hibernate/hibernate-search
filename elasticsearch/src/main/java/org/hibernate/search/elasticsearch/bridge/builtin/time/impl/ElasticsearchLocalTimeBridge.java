/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.bridge.builtin.time.impl;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.Locale;

import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Converts a {@link LocalTime} to a {@link String} in Elasticsearch's "strict_hour_minute_second_fraction" format.
 *
 * <p>Be aware that this format is <strong>not</strong> the same as {@link DateTimeFormatter#ISO_LOCAL_TIME}
 * (mainly because of the second fraction field, which is at least 3 characters long).
 *
 * @author Yoann Rodiere
 */
public class ElasticsearchLocalTimeBridge extends ElasticsearchTemporalAccessorStringBridge<LocalTime> {

	private static final Log LOG = LoggerFactory.make( Log.class );

	static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.appendValue( HOUR_OF_DAY, 2 )
			.appendLiteral( ':' )
			.appendValue( MINUTE_OF_HOUR, 2 )
			.optionalStart()
			.appendLiteral( ':' )
			.appendValue( SECOND_OF_MINUTE, 2 )
			.optionalStart()
			.appendFraction( NANO_OF_SECOND, 3, 9, true )
			.toFormatter( Locale.ROOT )
			.withResolverStyle( ResolverStyle.STRICT );

	public static final ElasticsearchLocalTimeBridge INSTANCE = new ElasticsearchLocalTimeBridge();

	private ElasticsearchLocalTimeBridge() {
		super( FORMATTER, LocalTime.class );
	}

	@Override
	LocalTime parse(DateTimeFormatter formatter, String stringValue) throws DateTimeException {
		return LocalTime.parse( stringValue, formatter );
	}

	@Override
	protected IllegalArgumentException createInvalidIndexNullAsException(String indexNullAs, DateTimeException e) {
		return LOG.invalidNullMarkerForLocalTime( e );
	}
}
