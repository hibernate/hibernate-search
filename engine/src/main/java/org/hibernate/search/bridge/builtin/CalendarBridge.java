/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin;

import org.hibernate.search.util.StringHelper;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.bridge.ParameterizedBridge;
import org.hibernate.search.bridge.TwoWayStringBridge;
import org.apache.lucene.document.DateTools;

import java.util.Date;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.text.ParseException;

public class CalendarBridge implements TwoWayStringBridge, ParameterizedBridge {

	public static final String RESOLUTION_PARAMETER = "resolution";

	private DateTools.Resolution resolution;
	public static final TwoWayStringBridge CALENDAR_YEAR = new CalendarBridge( Resolution.YEAR );
	public static final TwoWayStringBridge CALENDAR_MONTH = new CalendarBridge( Resolution.MONTH );
	public static final TwoWayStringBridge CALENDAR_DAY = new CalendarBridge( Resolution.DAY );
	public static final TwoWayStringBridge CALENDAR_HOUR = new CalendarBridge( Resolution.HOUR );
	public static final TwoWayStringBridge CALENDAR_MINUTE = new CalendarBridge( Resolution.MINUTE );
	public static final TwoWayStringBridge CALENDAR_SECOND = new CalendarBridge( Resolution.SECOND );
	public static final TwoWayStringBridge CALENDAR_MILLISECOND = new CalendarBridge( Resolution.MILLISECOND );

	public CalendarBridge() {
	}

	public CalendarBridge(Resolution resolution) {
		this.resolution = DateResolutionUtil.getLuceneResolution( resolution);
	}

	@Override
	public void setParameterValues(Map<String,String> parameters) {
		Object resolution = parameters.get( RESOLUTION_PARAMETER );
		Resolution hibResolution = Resolution.valueOf( ( (String) resolution ).toUpperCase( Locale.ENGLISH ) );
		this.resolution = DateResolutionUtil.getLuceneResolution( hibResolution );
	}

	@Override
	public Object stringToObject(String stringValue) {
		if ( StringHelper.isEmpty( stringValue ) ) {
			return null;
		}
		try {
			Date date = DateTools.stringToDate( stringValue );
			Calendar calendar = Calendar.getInstance();
			calendar.setTime( date );
			return calendar;
		}
		catch (ParseException e) {
			throw new SearchException( "Unable to parse into calendar: " + stringValue, e );
		}
	}

	@Override
	public String objectToString(Object object) {
		if ( object == null ) {
			return null;
		}
		Calendar calendar = (Calendar) object;
		return DateTools.dateToString( calendar.getTime(), resolution );
	}

}
