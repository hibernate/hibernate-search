/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.util.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.bind.DatatypeConverter;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;

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

	public static String calendarToString(Calendar calendar) {
		return DatatypeConverter.printDateTime( calendar );
	}

	public static Calendar stringToCalendar(String value) {
		return DatatypeConverter.parseDateTime( value );
	}

	/**
	 * Limit a calendar resolution.
	 *
	 * @param calendar The calendar whose resolution is to be limited
	 * @param resolution The desired resolution of the date to be returned
	 * @return the calendar with all values more precise than {@code resolution} set to 0 or 1
	 *
	 * @see DateTools#round(Date, Resolution)
	 */
	public static Calendar round(Calendar calendar, Resolution resolution) {
		final Calendar calInstance = (Calendar) calendar.clone();
		/*
		 * Make sure we keep the timezone: use a cloned version of the calendar
		 * to set the rounded time on it, not a new calendar instance.
		 */
		calInstance.setTime( DateTools.round( calInstance.getTime(), resolution ) );
		return calInstance;
	}

}
