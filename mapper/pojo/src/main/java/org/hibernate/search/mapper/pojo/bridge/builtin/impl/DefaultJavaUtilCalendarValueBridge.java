/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeContext;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

public final class DefaultJavaUtilCalendarValueBridge implements ValueBridge<Calendar, ZonedDateTime> {

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public StandardIndexFieldTypeContext<?, ZonedDateTime> bind(ValueBridgeBindingContext<Calendar> context) {
		return context.getTypeFactory().asZonedDateTime()
				.projectionConverter( PojoDefaultCalendarFromDocumentFieldValueConverter.INSTANCE );
	}

	@Override
	public ZonedDateTime toIndexedValue(Calendar value, ValueBridgeToIndexedValueContext context) {
		if ( value == null ) {
			return null;
		}

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
	public Calendar cast(Object value) {
		return (Calendar) value;
	}

	@Override
	public boolean isCompatibleWith(ValueBridge<?, ?> other) {
		return getClass().equals( other.getClass() );
	}

	private static class PojoDefaultCalendarFromDocumentFieldValueConverter
			implements FromDocumentFieldValueConverter<ZonedDateTime, Calendar> {
		private static final PojoDefaultCalendarFromDocumentFieldValueConverter INSTANCE = new PojoDefaultCalendarFromDocumentFieldValueConverter();

		@Override
		public boolean isConvertedTypeAssignableTo(Class<?> superTypeCandidate) {
			return superTypeCandidate.isAssignableFrom( Calendar.class );
		}

		@Override
		public Calendar convert(ZonedDateTime value, FromDocumentFieldValueConvertContext context) {
			return value == null ? null : from( value );
		}

		@Override
		public boolean isCompatibleWith(FromDocumentFieldValueConverter<?, ?> other) {
			return INSTANCE.equals( other );
		}

		private static Calendar from(ZonedDateTime value) {
			// We had some troubles using `GregorianCalendar.from( value )`:
			// it seems that the method calculates firstDayOfWeek and minimalDaysInFirstWeek
			// in a different way GregorianCalendar.getInstance does.
			Calendar calendar = Calendar.getInstance( TimeZone.getTimeZone( value.getZone() ), Locale.getDefault() );
			if ( calendar instanceof GregorianCalendar ) {
				calendar.setTimeInMillis( Math.addExact( Math.multiplyExact( value.toEpochSecond(), 1000 ), value.get( ChronoField.MILLI_OF_SECOND ) ) );
			}
			else {
				calendar.setTime( Date.from( value.toInstant() ) );
			}

			return calendar;
		}
	}
}
