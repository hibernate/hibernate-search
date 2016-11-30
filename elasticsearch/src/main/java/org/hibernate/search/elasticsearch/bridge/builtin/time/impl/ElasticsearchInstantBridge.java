/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.bridge.builtin.time.impl;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.Locale;

import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Converts a {@link Instant} to a {@link String} in ISO-8601 extended format (9 digits for the year instead of 4).
 *
 * <p>The formatting is done using the UTC timezone.
 *
 * <p>Be aware that this format is <strong>not</strong> the same as {@link DateTimeFormatter#ISO_INSTANT}
 * (mainly because of the second fraction field, which is at least 3 characters long), nor as Elasticsearch's
 * "strict_date_optional_time" format (since years with more than 4 digits are allowed).
 *
 * @author Yoann Rodiere
 */
public class ElasticsearchInstantBridge extends ElasticsearchTemporalAccessorStringBridge<Instant> {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.append( ElasticsearchLocalDateBridge.FORMATTER )
			.appendLiteral( 'T' )
			.optionalStart()
			.append( ElasticsearchLocalTimeBridge.FORMATTER )
			.optionalStart()
			.appendOffsetId()
			.toFormatter( Locale.ROOT )
			.withResolverStyle( ResolverStyle.STRICT );

	private static final DateTimeFormatter INDEX_NULL_AS_FORMATTER = new DateTimeFormatterBuilder()
			.append( ElasticsearchLocalDateBridge.FORMATTER )
			.appendLiteral( 'T' )
			.optionalStart()
			.append( ElasticsearchLocalTimeBridge.FORMATTER )
			.optionalStart()
			.appendOffsetId()
			.optionalStart()
			.appendLiteral( '[' )
			.appendZoneId()
			.appendLiteral( ']' )
			.toFormatter( Locale.ROOT )
			.withResolverStyle( ResolverStyle.STRICT );

	public static final ElasticsearchInstantBridge INSTANCE = new ElasticsearchInstantBridge();

	private ElasticsearchInstantBridge() {
		super( FORMATTER, Instant.class, INDEX_NULL_AS_FORMATTER );
	}

	@Override
	String format(DateTimeFormatter formatter, Instant object) {
		return formatter.format( object == null ? null : object.atOffset( ZoneOffset.UTC ) );
	}

	@Override
	Instant parse(DateTimeFormatter formatter, String stringValue) throws DateTimeException {
		return Instant.from( formatter.parse( stringValue ) );
	}

	@Override
	protected IllegalArgumentException createInvalidIndexNullAsException(String indexNullAs, DateTimeException e) {
		return LOG.invalidNullMarkerForInstant( e );
	}
}
