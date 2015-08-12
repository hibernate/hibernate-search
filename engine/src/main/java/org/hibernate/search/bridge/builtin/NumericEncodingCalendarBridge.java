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
 * Bridge a {@code java.util.Date} truncated to the specified resolution to a numerically indexed {@code long}.
 *
 * GMT is used as time zone.
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
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class NumericEncodingCalendarBridge extends NumericEncodingDateBridge {

	private static final TimeZone ENCODING_TIME_ZONE = TimeZone.getTimeZone( "UTC" );

	public static final TwoWayFieldBridge DATE_YEAR = new NumericEncodingCalendarBridge( Resolution.YEAR );
	public static final TwoWayFieldBridge DATE_MONTH = new NumericEncodingCalendarBridge( Resolution.MONTH );
	public static final TwoWayFieldBridge DATE_DAY = new NumericEncodingCalendarBridge( Resolution.DAY );
	public static final TwoWayFieldBridge DATE_HOUR = new NumericEncodingCalendarBridge( Resolution.HOUR );
	public static final TwoWayFieldBridge DATE_MINUTE = new NumericEncodingCalendarBridge( Resolution.MINUTE );
	public static final TwoWayFieldBridge DATE_SECOND = new NumericEncodingCalendarBridge( Resolution.SECOND );
	public static final TwoWayFieldBridge DATE_MILLISECOND = new NumericEncodingCalendarBridge(
			Resolution.MILLISECOND
	);

	public NumericEncodingCalendarBridge() {
		super( Resolution.MILLISECOND );
	}

	public NumericEncodingCalendarBridge(Resolution resolution) {
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
