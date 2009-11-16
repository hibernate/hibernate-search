package org.hibernate.search.bridge.builtin;

import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.bridge.ParameterizedBridge;
import org.hibernate.search.bridge.TwoWayStringBridge;
import org.hibernate.util.StringHelper;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.apache.lucene.document.DateTools;

import java.util.Date;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.text.ParseException;


public class CalendarBridge implements TwoWayStringBridge, ParameterizedBridge {
    
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

	public void setParameterValues(Map parameters) {
		Object resolution = parameters.get( "resolution" );
		Resolution hibResolution;
		if ( resolution instanceof String ) {
			hibResolution = Resolution.valueOf( ( (String) resolution ).toUpperCase( Locale.ENGLISH ) );
		}
		else {
			hibResolution = (Resolution) resolution;
		}
		resolution = DateResolutionUtil.getLuceneResolution( hibResolution );
	}

	
	
    public Object stringToObject(String stringValue) {
        if ( StringHelper.isEmpty( stringValue )) {
            return null;
        }
        try {
            Date date = DateTools.stringToDate(stringValue);
            Calendar calendar =  Calendar.getInstance();
            calendar.setTime(date);
            return calendar;
        } catch (ParseException e) {
            throw new HibernateException( "Unable to parse into calendar: " + stringValue, e );
        }
	}

	public String objectToString(Object object) {
        if (object == null) {
            return null;
        }
        Calendar calendar = (Calendar)object;
        return DateTools.dateToString(calendar.getTime(),resolution);
    }

}
