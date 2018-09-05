/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.lucene.document.Document;

import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;

/**
 * Bridge a {@code java.util.Calendar} to a {@code String}, truncated to the specified resolution.
 * <ul>
 * <li>Resolution.YEAR: yyyy</li>
 * <li>Resolution.MONTH: yyyyMM</li>
 * <li>Resolution.DAY: yyyyMMdd</li>
 * <li>Resolution.HOUR: yyyyMMddHH</li>
 * <li>Resolution.MINUTE: yyyyMMddHHmm</li>
 * <li>Resolution.SECOND: yyyyMMddHHmmss</li>
 * <li>Resolution.MILLISECOND: yyyyMMddHHmmssSSS</li>
 * </ul>
 *
 * @author Davide D'Alto
 */
public class StringEncodingCalendarBridge extends StringEncodingDateBridge {

	public static final TwoWayFieldBridge CALENDAR_YEAR = new StringEncodingCalendarBridge( Resolution.YEAR );
	public static final TwoWayFieldBridge CALENDAR_MONTH = new StringEncodingCalendarBridge( Resolution.MONTH );
	public static final TwoWayFieldBridge CALENDAR_DAY = new StringEncodingCalendarBridge( Resolution.DAY );
	public static final TwoWayFieldBridge CALENDAR_HOUR = new StringEncodingCalendarBridge( Resolution.HOUR );
	public static final TwoWayFieldBridge CALENDAR_MINUTE = new StringEncodingCalendarBridge( Resolution.MINUTE );
	public static final TwoWayFieldBridge CALENDAR_SECOND = new StringEncodingCalendarBridge( Resolution.SECOND );
	public static final TwoWayFieldBridge CALENDAR_MILLISECOND = new StringEncodingCalendarBridge(
			Resolution.MILLISECOND
	);

	private static final TimeZone ENCODING_TIME_ZONE = TimeZone.getTimeZone( "UTC" );

	public StringEncodingCalendarBridge() {
	}

	public StringEncodingCalendarBridge(Resolution resolution) {
		super( resolution );
	}

	@Override
	public Object get(String name, Document document) {
		Object value = super.get( name, document );
		if ( value != null ) {
			Calendar calendar = Calendar.getInstance( ENCODING_TIME_ZONE, Locale.ROOT );
			calendar.setTime( (Date) value );
			value = calendar;
		}
		return value;
	}

	@Override
	public String objectToString(Object object) {
		Date date = null;
		if ( object != null ) {
			date = ( (Calendar) object ).getTime();
		}
		return super.objectToString( date );
	}

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		Date date = null;
		if ( value != null ) {
			date = ( (Calendar) value ).getTime();
		}
		super.set( name, date, document, luceneOptions );
	}
}
