/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.util.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.bind.DatatypeConverter;

/**
 * Various utilities to manipulate dates and comply with Elasticsearch date format.
 *
 * @author Guillaume Smet
 */
public final class ElasticsearchDateHelper {

	private static final TimeZone ENCODING_TIME_ZONE = TimeZone.getTimeZone( "UTC" );

	private ElasticsearchDateHelper() {
		// Not allowed
	}

	public static Date stringToDate(String value) {
		Calendar c = DatatypeConverter.parseDateTime( value );
		return c.getTime();
	}

	public static String dateToString(Date date) {
		Calendar c = Calendar.getInstance( ENCODING_TIME_ZONE, Locale.ENGLISH );
		c.setTime( date );
		return DatatypeConverter.printDateTime( c );
	}

	public static Calendar dateToCalendar(Date date) {
		Calendar calendar = Calendar.getInstance( ENCODING_TIME_ZONE, Locale.ENGLISH );
		calendar.setTime( date );
		return calendar;
	}

	public static String calendarToString(Calendar calendar) {
		Calendar c = (Calendar) calendar.clone();
		c.setTimeZone( ENCODING_TIME_ZONE );
		return DatatypeConverter.printDateTime( c );
	}

	public static Calendar stringToCalendar(String value) {
		return DatatypeConverter.parseDateTime( value );
	}

}
