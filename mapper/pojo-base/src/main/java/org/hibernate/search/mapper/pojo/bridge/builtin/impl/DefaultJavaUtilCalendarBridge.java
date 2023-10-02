/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public final class DefaultJavaUtilCalendarBridge extends AbstractConvertingDelegatingDefaultBridge<Calendar, ZonedDateTime> {

	public static final DefaultJavaUtilCalendarBridge INSTANCE = new DefaultJavaUtilCalendarBridge();

	public DefaultJavaUtilCalendarBridge() {
		super( DefaultZonedDateTimeBridge.INSTANCE );
	}

	@Override
	protected ZonedDateTime toConvertedValue(Calendar value) {
		if ( value instanceof GregorianCalendar ) {
			return ( (GregorianCalendar) value ).toZonedDateTime();
		}

		// Using any static zone id to produce the ZonedDateTime instance,
		// since not-gregorian calendar could not contain this time zone information.
		// We know that data stored in the backends will contain this extra non-user data,
		// but we accept this compromise considering that Calendar is considered a Java legacy type
		// and most of the time it is used as a GregorianCalendar implementation.
		return value.toInstant().atZone( ZoneId.of( "UTC" ) );
	}

	@Override
	protected Calendar fromConvertedValue(ZonedDateTime value) {
		// We had some troubles using `GregorianCalendar.from( value )`:
		// it seems that the method calculates firstDayOfWeek and minimalDaysInFirstWeek
		// in a different way GregorianCalendar.getInstance does.
		Calendar calendar = Calendar.getInstance( TimeZone.getTimeZone( value.getZone() ), Locale.getDefault() );
		if ( calendar instanceof GregorianCalendar ) {
			calendar.setTimeInMillis( Math.addExact( Math.multiplyExact( value.toEpochSecond(), 1000L ),
					value.get( ChronoField.MILLI_OF_SECOND ) ) );
		}
		else {
			calendar.setTime( Date.from( value.toInstant() ) );
		}

		return calendar;
	}

}
