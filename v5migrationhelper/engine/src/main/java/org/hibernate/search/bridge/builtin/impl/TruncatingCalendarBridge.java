/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.bridge.builtin.impl;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

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
public class TruncatingCalendarBridge implements ValueBridge<Calendar, ZonedDateTime> {

	private final Truncation truncation;

	public TruncatingCalendarBridge(Truncation truncation) {
		this.truncation = truncation;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public ZonedDateTime toIndexedValue(Calendar value, ValueBridgeToIndexedValueContext context) {
		if ( value == null ) {
			return null;
		}
		return truncate( toZonedDateTime( value ) );
	}

	@Override
	public Calendar fromIndexedValue(ZonedDateTime value, ValueBridgeFromIndexedValueContext context) {
		return value == null ? null : fromZonedDateTime( value );
	}

	@Override
	public ZonedDateTime parse(String value) {
		return truncate( ParseUtils.parseZonedDateTime( value ) );
	}

	@Override
	public boolean isCompatibleWith(ValueBridge<?, ?> other) {
		return getClass().equals( other.getClass() )
				&& truncation.equals( ( (TruncatingCalendarBridge) other ).truncation );
	}

	private ZonedDateTime truncate(ZonedDateTime value) {
		// Search 5 used to convert all calendars to UTC before truncating.
		// A dubious choice for sure, but we need to do the same if we want truncation to behave the same.
		ZoneId originalZone = value.getZone();
		return truncation.truncate( value.withZoneSameInstant( ZoneOffset.UTC ) )
				.withZoneSameInstant( originalZone );
	}

	// Code copied from org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultJavaUtilCalendarValueBridge
	private static ZonedDateTime toZonedDateTime(Calendar value) {
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

	// Code copied from org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultJavaUtilCalendarValueBridge
	private static Calendar fromZonedDateTime(ZonedDateTime value) {
		// We had some troubles using `GregorianCalendar.from( value )`:
		// it seems that the method calculates firstDayOfWeek and minimalDaysInFirstWeek
		// in a different way GregorianCalendar.getInstance does.
		Calendar calendar = Calendar.getInstance( TimeZone.getTimeZone( value.getZone() ), Locale.getDefault() );
		if ( calendar instanceof GregorianCalendar ) {
			calendar.setTimeInMillis( Math.addExact(
					Math.multiplyExact( value.toEpochSecond(), 1000L ),
					value.get( ChronoField.MILLI_OF_SECOND ) ) );
		}
		else {
			calendar.setTime( Date.from( value.toInstant() ) );
		}

		return calendar;
	}
}
