/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.bridge.builtin.time.impl;

import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.Locale;

import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Converts a {@link OffsetDateTime} to a {@link String} in ISO-8601 extended format (9 digits for the year instead of 4).
 *
 * <p>Be aware that this format is <strong>not</strong> the same as {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME}
 * (mainly because of the second fraction field, which is at least 3 characters long), nor as Elasticsearch's
 * "strict_date_time" format (since years with more than 4 digits are allowed).
 *
 * @author Yoann Rodiere
 */
public class ElasticsearchOffsetDateTimeBridge extends ElasticsearchTemporalAccessorStringBridge<OffsetDateTime> {

	private static final Log LOG = LoggerFactory.make( Log.class );

	static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.append( ElasticsearchLocalDateTimeBridge.FORMATTER )
			.appendOffsetId()
			.toFormatter( Locale.ROOT )
			.withResolverStyle( ResolverStyle.STRICT );

	public static final ElasticsearchOffsetDateTimeBridge INSTANCE = new ElasticsearchOffsetDateTimeBridge();

	private ElasticsearchOffsetDateTimeBridge() {
		super( FORMATTER, OffsetDateTime.class );
	}

	@Override
	OffsetDateTime parse(DateTimeFormatter formatter, String stringValue) throws DateTimeException {
		return OffsetDateTime.parse( stringValue, formatter );
	}

	@Override
	protected IllegalArgumentException createInvalidIndexNullAsException(String indexNullAs, DateTimeException e) {
		return LOG.invalidNullMarkerForOffsetDateTime( e );
	}
}
