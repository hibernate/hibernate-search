/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.bridge.builtin;

import org.hibernate.search.util.StringHelper;
import org.hibernate.search.SearchException;
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
