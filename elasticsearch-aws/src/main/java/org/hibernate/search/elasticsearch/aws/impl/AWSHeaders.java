/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.aws.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * @author Yoann Rodiere
 */
class AWSHeaders {

	private AWSHeaders() {
		// Private, this is a utils class
	}

	public static final String AUTHORIZATION = "authorization";
	public static final String HOST = "host";
	public static final String X_AMZ_DATE_HEADER_NAME = "x-amz-date";
	public static final String X_AMZ_CONTENT_SHA256_HEADER_NAME = "x-amz-content-sha256";

	private static final DateTimeFormatter AMZ_DATE_FORMATTER = new DateTimeFormatterBuilder()
			.appendValue( ChronoField.YEAR, 4 )
			.appendValue( ChronoField.MONTH_OF_YEAR, 2 )
			.appendValue( ChronoField.DAY_OF_MONTH, 2 )
			.appendLiteral( 'T' )
			.appendValue( ChronoField.HOUR_OF_DAY, 2 )
			.appendValue( ChronoField.MINUTE_OF_HOUR, 2 )
			.appendValue( ChronoField.SECOND_OF_MINUTE, 2 )
			.appendLiteral( 'Z' )
			.toFormatter();

	public static String toAmzDate(LocalDateTime dateTime) {
		return AMZ_DATE_FORMATTER.format( dateTime );
	}

}
