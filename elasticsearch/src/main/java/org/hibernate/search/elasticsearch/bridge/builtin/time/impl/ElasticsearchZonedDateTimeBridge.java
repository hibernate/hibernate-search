/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.bridge.builtin.time.impl;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

import org.hibernate.search.util.impl.TimeHelper;

/**
 * Converts a {@link ZonedDateTime} to a {@link String} almost in ISO-8601 extended format (9 digits
 * for the year instead of 4, and the zone ID is appended after a dash instead of being between squarebrackets).
 *
 * <p>Be aware that this format is <strong>not</strong> the same as {@link DateTimeFormatter#ISO_ZONED_DATE_TIME}
 * (mainly because of the second fraction field, which is at least 3 characters long, and because of the zone ID),
 * nor as Elasticsearch's "strict_date_time" format (since years with more than 4 digits are allowed, and both
 * the zone ID and offset are displayed).
 *
 * @author Yoann Rodiere
 */
public class ElasticsearchZonedDateTimeBridge extends ElasticsearchTemporalAccessorStringBridge<ZonedDateTime> {

	private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.append( ElasticsearchOffsetDateTimeBridge.FORMATTER )
			.appendLiteral( '-' )
			.parseCaseSensitive()
			.appendZoneRegionId()
			.toFormatter( Locale.ROOT )
			.withResolverStyle( ResolverStyle.STRICT );

	public static final ElasticsearchZonedDateTimeBridge INSTANCE = new ElasticsearchZonedDateTimeBridge();

	private ElasticsearchZonedDateTimeBridge() {
		super( FORMATTER, ZonedDateTime.class );
	}

	@Override
	ZonedDateTime parse(DateTimeFormatter formatter, String stringValue) throws DateTimeParseException {
		return TimeHelper.parseZoneDateTime( stringValue, formatter );
	}
}
