/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.bridge.builtin.time.impl;

import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

/**
 * Converts a {@link OffsetTime} to a {@link String} in Elasticsearch's "strict_time" format.
 *
 * <p>Be aware that this format is <strong>not</strong> the same as {@link DateTimeFormatter#ISO_OFFSET_TIME}
 * (mainly because of the second fraction field, which is at least 3 characters long).
 *
 * @author Yoann Rodiere
 */
public class ElasticsearchOffsetTimeBridge extends ElasticsearchTemporalAccessorStringBridge<OffsetTime> {

	private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.append( ElasticsearchLocalTimeBridge.FORMATTER )
			.appendOffsetId()
			.toFormatter( Locale.ROOT )
			.withResolverStyle( ResolverStyle.STRICT );

	public static final ElasticsearchOffsetTimeBridge INSTANCE = new ElasticsearchOffsetTimeBridge();

	private ElasticsearchOffsetTimeBridge() {
		super( FORMATTER, OffsetTime.class );
	}

	@Override
	OffsetTime parse(DateTimeFormatter formatter, String stringValue) throws DateTimeParseException {
		return OffsetTime.parse( stringValue, formatter );
	}
}
