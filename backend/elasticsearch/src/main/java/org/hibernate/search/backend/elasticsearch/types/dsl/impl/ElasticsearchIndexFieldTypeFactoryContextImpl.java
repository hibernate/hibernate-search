/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.lang.invoke.MethodHandles;
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

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchIndexFieldTypeFactoryContext;
import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchNativeIndexFieldTypeOptionsStep;
import org.hibernate.search.backend.elasticsearch.types.format.impl.ElasticsearchDefaultFieldFormatProvider;
import org.hibernate.search.engine.backend.types.dsl.ScaledNumberIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.Gson;



public class ElasticsearchIndexFieldTypeFactoryContextImpl
		implements ElasticsearchIndexFieldTypeFactoryContext, ElasticsearchIndexFieldTypeBuildContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final EventContext eventContext;
	private final Gson userFacingGson;
	private final ElasticsearchDefaultFieldFormatProvider defaultFieldFormatProvider;
	private final IndexFieldTypeDefaultsProvider typeDefaultsProvider;

	public ElasticsearchIndexFieldTypeFactoryContextImpl(EventContext eventContext, Gson userFacingGson,
			ElasticsearchDefaultFieldFormatProvider defaultFieldFormatProvider,
			IndexFieldTypeDefaultsProvider typeDefaultsProvider) {
		this.eventContext = eventContext;
		this.userFacingGson = userFacingGson;
		this.defaultFieldFormatProvider = defaultFieldFormatProvider;
		this.typeDefaultsProvider = typeDefaultsProvider;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <F> StandardIndexFieldTypeOptionsStep<?, F> as(Class<F> valueType) {
		if ( String.class.equals( valueType ) ) {
			return (StandardIndexFieldTypeOptionsStep<?, F>) asString();
		}
		else if ( Integer.class.equals( valueType ) ) {
			return (StandardIndexFieldTypeOptionsStep<?, F>) asInteger();
		}
		else if ( Long.class.equals( valueType ) ) {
			return (StandardIndexFieldTypeOptionsStep<?, F>) asLong();
		}
		else if ( Boolean.class.equals( valueType ) ) {
			return (StandardIndexFieldTypeOptionsStep<?, F>) asBoolean();
		}
		else if ( Byte.class.equals( valueType ) ) {
			return (StandardIndexFieldTypeOptionsStep<?, F>) asByte();
		}
		else if ( Short.class.equals( valueType ) ) {
			return (StandardIndexFieldTypeOptionsStep<?, F>) asShort();
		}
		else if ( Float.class.equals( valueType ) ) {
			return (StandardIndexFieldTypeOptionsStep<?, F>) asFloat();
		}
		else if ( Double.class.equals( valueType ) ) {
			return (StandardIndexFieldTypeOptionsStep<?, F>) asDouble();
		}
		else if ( LocalDate.class.equals( valueType ) ) {
			return (StandardIndexFieldTypeOptionsStep<?, F>) asLocalDate();
		}
		else if ( LocalDateTime.class.equals( valueType ) ) {
			return (StandardIndexFieldTypeOptionsStep<?, F>) asLocalDateTime();
		}
		else if ( LocalTime.class.equals( valueType ) ) {
			return (StandardIndexFieldTypeOptionsStep<?, F>) asLocalTime();
		}
		else if ( Instant.class.equals( valueType ) ) {
			return (StandardIndexFieldTypeOptionsStep<?, F>) asInstant();
		}
		else if ( ZonedDateTime.class.equals( valueType ) ) {
			return (StandardIndexFieldTypeOptionsStep<?, F>) asZonedDateTime();
		}
		else if ( Year.class.equals( valueType ) ) {
			return (StandardIndexFieldTypeOptionsStep<?, F>) asYear();
		}
		else if ( YearMonth.class.equals( valueType ) ) {
			return (StandardIndexFieldTypeOptionsStep<?, F>) asYearMonth();
		}
		else if ( MonthDay.class.equals( valueType ) ) {
			return (StandardIndexFieldTypeOptionsStep<?, F>) asMonthDay();
		}
		else if ( OffsetDateTime.class.equals( valueType ) ) {
			return (StandardIndexFieldTypeOptionsStep<?, F>) asOffsetDateTime();
		}
		else if ( OffsetTime.class.equals( valueType ) ) {
			return (StandardIndexFieldTypeOptionsStep<?, F>) asOffsetTime();
		}
		else if ( GeoPoint.class.equals( valueType ) ) {
			return (StandardIndexFieldTypeOptionsStep<?, F>) asGeoPoint();
		}
		else if ( BigDecimal.class.equals( valueType ) ) {
			return (StandardIndexFieldTypeOptionsStep<?, F>) asBigDecimal();
		}
		else if ( BigInteger.class.equals( valueType ) ) {
			return (StandardIndexFieldTypeOptionsStep<?, F>) asBigInteger();
		}
		else {
			throw log.cannotGuessFieldType( valueType, getEventContext() );
		}
	}

	@Override
	public StringIndexFieldTypeOptionsStep<?> asString() {
		return new ElasticsearchStringIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, Integer> asInteger() {
		return new ElasticsearchIntegerIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, Long> asLong() {
		return new ElasticsearchLongIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, Boolean> asBoolean() {
		return new ElasticsearchBooleanIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, Byte> asByte() {
		return new ElasticsearchByteIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, Short> asShort() {
		return new ElasticsearchShortIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, Float> asFloat() {
		return new ElasticsearchFloatIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, Double> asDouble() {
		return new ElasticsearchDoubleIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, LocalDate> asLocalDate() {
		return new ElasticsearchLocalDateIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, LocalDateTime> asLocalDateTime() {
		return new ElasticsearchLocalDateTimeIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, LocalTime> asLocalTime() {
		return new ElasticsearchLocalTimeIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, Instant> asInstant() {
		return new ElasticsearchInstantIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, ZonedDateTime> asZonedDateTime() {
		return new ElasticsearchZonedDateTimeIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, Year> asYear() {
		return new ElasticsearchYearIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, YearMonth> asYearMonth() {
		return new ElasticsearchYearMonthIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, MonthDay> asMonthDay() {
		return new ElasticsearchMonthDayIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, OffsetDateTime> asOffsetDateTime() {
		return new ElasticsearchOffsetDateTimeIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, OffsetTime> asOffsetTime() {
		return new ElasticsearchOffsetTimeIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, GeoPoint> asGeoPoint() {
		return new ElasticsearchGeoPointIndexFieldTypeOptionsStep( this );
	}

	@Override
	public ScaledNumberIndexFieldTypeOptionsStep<?, BigDecimal> asBigDecimal() {
		return new ElasticsearchBigDecimalIndexFieldTypeOptionsStep( this, typeDefaultsProvider );
	}

	@Override
	public ScaledNumberIndexFieldTypeOptionsStep<?, BigInteger> asBigInteger() {
		return new ElasticsearchBigIntegerIndexFieldTypeOptionsStep( this, typeDefaultsProvider );
	}

	@Override
	public ElasticsearchNativeIndexFieldTypeOptionsStep<?> asNative(String mappingJsonString) {
		return new ElasticsearchNativeIndexFieldTypeOptionsStepImpl( this, mappingJsonString );
	}

	@Override
	public EventContext getEventContext() {
		return eventContext;
	}

	@Override
	public Gson getUserFacingGson() {
		return userFacingGson;
	}

	@Override
	public ElasticsearchDefaultFieldFormatProvider getDefaultFieldFormatProvider() {
		return defaultFieldFormatProvider;
	}
}
