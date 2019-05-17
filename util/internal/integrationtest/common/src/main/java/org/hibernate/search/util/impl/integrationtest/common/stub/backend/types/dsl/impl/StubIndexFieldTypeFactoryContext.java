/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.dsl.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactoryContext;
import org.hibernate.search.engine.backend.types.dsl.ScaledNumberIndexFieldTypeContext;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeContext;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.engine.spatial.GeoPoint;

public class StubIndexFieldTypeFactoryContext implements IndexFieldTypeFactoryContext {

	private final IndexFieldTypeDefaultsProvider defaultsProvider;

	public StubIndexFieldTypeFactoryContext(IndexFieldTypeDefaultsProvider defaultsProvider) {
		this.defaultsProvider = defaultsProvider;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <F> StandardIndexFieldTypeContext<?, F> as(Class<F> inputType) {
		if ( String.class.isAssignableFrom( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asString();
		}
		else if ( BigDecimal.class.isAssignableFrom( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asBigDecimal();
		}
		else {
			return new StubGenericIndexFieldTypeContext<>( inputType );
		}
	}

	@Override
	public StringIndexFieldTypeContext<?> asString() {
		return new StubStringIndexFieldTypeContext();
	}

	@Override
	public StandardIndexFieldTypeContext<?, Integer> asInteger() {
		return as( Integer.class );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Long> asLong() {
		return as( Long.class );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Boolean> asBoolean() {
		return as( Boolean.class );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Byte> asByte() {
		return as( Byte.class );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Short> asShort() {
		return as( Short.class );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Float> asFloat() {
		return as( Float.class );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Double> asDouble() {
		return as( Double.class );
	}

	@Override
	public StandardIndexFieldTypeContext<?, LocalDate> asLocalDate() {
		return as( LocalDate.class );
	}

	@Override
	public StandardIndexFieldTypeContext<?, LocalDateTime> asLocalDateTime() {
		return as( LocalDateTime.class );
	}

	@Override
	public StandardIndexFieldTypeContext<?, LocalTime> asLocalTime() {
		return as( LocalTime.class );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Instant> asInstant() {
		return as( Instant.class );
	}

	@Override
	public StandardIndexFieldTypeContext<?, ZonedDateTime> asZonedDateTime() {
		return as( ZonedDateTime.class );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Year> asYear() {
		return as( Year.class );
	}

	@Override
	public StandardIndexFieldTypeContext<?, YearMonth> asYearMonth() {
		return as( YearMonth.class );
	}

	@Override
	public StandardIndexFieldTypeContext<?, MonthDay> asMonthDay() {
		return as( MonthDay.class );
	}

	@Override
	public StandardIndexFieldTypeContext<?, OffsetDateTime> asOffsetDateTime() {
		return as( OffsetDateTime.class );
	}

	@Override
	public StandardIndexFieldTypeContext<?, OffsetTime> asOffsetTime() {
		return as( OffsetTime.class );
	}

	@Override
	public StandardIndexFieldTypeContext<?, GeoPoint> asGeoPoint() {
		return as( GeoPoint.class );
	}

	@Override
	public ScaledNumberIndexFieldTypeContext<?, BigDecimal> asBigDecimal() {
		return new StubScaledNumberIndexFieldTypeContext( defaultsProvider );
	}
}
