/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.dsl.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
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

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.types.dsl.ScaledNumberIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.types.dsl.VectorFieldTypeOptionsStep;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.engine.spatial.GeoPoint;

public class StubIndexFieldTypeFactory implements IndexFieldTypeFactory {

	private final IndexFieldTypeDefaultsProvider defaultsProvider;

	public StubIndexFieldTypeFactory(IndexFieldTypeDefaultsProvider defaultsProvider) {
		this.defaultsProvider = defaultsProvider;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <F> StandardIndexFieldTypeOptionsStep<?, F> as(Class<F> valueType) {
		if ( String.class.isAssignableFrom( valueType ) ) {
			return (StandardIndexFieldTypeOptionsStep<?, F>) asString();
		}
		else if ( BigDecimal.class.isAssignableFrom( valueType ) ) {
			return (StandardIndexFieldTypeOptionsStep<?, F>) asBigDecimal();
		}
		else if ( BigInteger.class.isAssignableFrom( valueType ) ) {
			return (StandardIndexFieldTypeOptionsStep<?, F>) asBigInteger();
		}
		else {
			return new StubGenericStandardIndexFieldTypeOptionsStep<>( valueType );
		}
	}

	@Override
	public StringIndexFieldTypeOptionsStep<?> asString() {
		return new StubStringIndexFieldTypeOptionsStep();
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, Integer> asInteger() {
		return as( Integer.class );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, Long> asLong() {
		return as( Long.class );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, Boolean> asBoolean() {
		return as( Boolean.class );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, Byte> asByte() {
		return as( Byte.class );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, Short> asShort() {
		return as( Short.class );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, Float> asFloat() {
		return as( Float.class );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, Double> asDouble() {
		return as( Double.class );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, LocalDate> asLocalDate() {
		return as( LocalDate.class );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, LocalDateTime> asLocalDateTime() {
		return as( LocalDateTime.class );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, LocalTime> asLocalTime() {
		return as( LocalTime.class );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, Instant> asInstant() {
		return as( Instant.class );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, ZonedDateTime> asZonedDateTime() {
		return as( ZonedDateTime.class );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, Year> asYear() {
		return as( Year.class );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, YearMonth> asYearMonth() {
		return as( YearMonth.class );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, MonthDay> asMonthDay() {
		return as( MonthDay.class );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, OffsetDateTime> asOffsetDateTime() {
		return as( OffsetDateTime.class );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, OffsetTime> asOffsetTime() {
		return as( OffsetTime.class );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, GeoPoint> asGeoPoint() {
		return as( GeoPoint.class );
	}

	@Override
	public ScaledNumberIndexFieldTypeOptionsStep<?, BigDecimal> asBigDecimal() {
		return new StubScaledNumberIndexFieldTypeOptionsStep<>( BigDecimal.class, defaultsProvider );
	}

	@Override
	public ScaledNumberIndexFieldTypeOptionsStep<?, BigInteger> asBigInteger() {
		return new StubScaledNumberIndexFieldTypeOptionsStep<>( BigInteger.class, defaultsProvider );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <F> VectorFieldTypeOptionsStep<?, F> asVector(Class<F> valueType) {
		if ( byte[].class.equals( valueType ) ) {
			return (VectorFieldTypeOptionsStep<?, F>) asByteVector();
		}
		if ( float[].class.equals( valueType ) ) {
			return (VectorFieldTypeOptionsStep<?, F>) asFloatVector();
		}
		throw new IllegalStateException( "Unsupported vector type " + valueType );
	}

	@Override
	public VectorFieldTypeOptionsStep<?, byte[]> asByteVector() {
		return new StubVectorFieldTypeOptionsStep<>( byte[].class );
	}

	@Override
	public VectorFieldTypeOptionsStep<?, float[]> asFloatVector() {
		return new StubVectorFieldTypeOptionsStep<>( float[].class );
	}

	public <T> IndexFieldTypeOptionsStep<?, T> asNonStandard(Class<T> fieldValueType) {
		return new StubGenericNonStandardIndexFieldTypeOptionsStep<>( fieldValueType );
	}
}
