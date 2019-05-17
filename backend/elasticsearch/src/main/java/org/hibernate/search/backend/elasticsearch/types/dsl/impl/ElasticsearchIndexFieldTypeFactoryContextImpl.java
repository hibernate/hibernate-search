/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.lang.invoke.MethodHandles;
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

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchIndexFieldTypeFactoryContext;
import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchNativeIndexFieldTypeContext;
import org.hibernate.search.backend.elasticsearch.types.format.impl.ElasticsearchDefaultFieldFormatProvider;
import org.hibernate.search.engine.backend.types.dsl.ScaledNumberIndexFieldTypeContext;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeContext;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.Gson;


/**
 * @author Yoann Rodiere
 */
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
	public <F> StandardIndexFieldTypeContext<?, F> as(Class<F> inputType) {
		if ( String.class.equals( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asString();
		}
		else if ( Integer.class.equals( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asInteger();
		}
		else if ( Long.class.equals( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asLong();
		}
		else if ( Boolean.class.equals( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asBoolean();
		}
		else if ( Byte.class.equals( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asByte();
		}
		else if ( Short.class.equals( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asShort();
		}
		else if ( Float.class.equals( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asFloat();
		}
		else if ( Double.class.equals( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asDouble();
		}
		else if ( LocalDate.class.equals( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asLocalDate();
		}
		else if ( LocalDateTime.class.equals( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asLocalDateTime();
		}
		else if ( LocalTime.class.equals( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asLocalTime();
		}
		else if ( Instant.class.equals( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asInstant();
		}
		else if ( ZonedDateTime.class.equals( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asZonedDateTime();
		}
		else if ( Year.class.equals( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asYear();
		}
		else if ( YearMonth.class.equals( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asYearMonth();
		}
		else if ( MonthDay.class.equals( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asMonthDay();
		}
		else if ( OffsetDateTime.class.equals( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asOffsetDateTime();
		}
		else if ( OffsetTime.class.equals( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asOffsetTime();
		}
		else if ( GeoPoint.class.equals( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asGeoPoint();
		}
		else if ( BigDecimal.class.equals( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asBigDecimal();
		}
		else {
			// TODO implement other types
			throw log.cannotGuessFieldType( inputType, getEventContext() );
		}
	}

	@Override
	public StringIndexFieldTypeContext<?> asString() {
		return new ElasticsearchStringIndexFieldTypeContext( this );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Integer> asInteger() {
		return new ElasticsearchIntegerIndexFieldTypeContext( this );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Long> asLong() {
		return new ElasticsearchLongIndexFieldTypeContext( this );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Boolean> asBoolean() {
		return new ElasticsearchBooleanIndexFieldTypeContext( this );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Byte> asByte() {
		return new ElasticsearchByteIndexFieldTypeContext( this );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Short> asShort() {
		return new ElasticsearchShortIndexFieldTypeContext( this );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Float> asFloat() {
		return new ElasticsearchFloatIndexFieldTypeContext( this );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Double> asDouble() {
		return new ElasticsearchDoubleIndexFieldTypeContext( this );
	}

	@Override
	public StandardIndexFieldTypeContext<?, LocalDate> asLocalDate() {
		return new ElasticsearchLocalDateIndexFieldTypeContext( this );
	}

	@Override
	public StandardIndexFieldTypeContext<?, LocalDateTime> asLocalDateTime() {
		return new ElasticsearchLocalDateTimeIndexFieldTypeContext( this );
	}

	@Override
	public StandardIndexFieldTypeContext<?, LocalTime> asLocalTime() {
		return new ElasticsearchLocalTimeIndexFieldTypeContext( this );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Instant> asInstant() {
		return new ElasticsearchInstantIndexFieldTypeContext( this );
	}

	@Override
	public StandardIndexFieldTypeContext<?, ZonedDateTime> asZonedDateTime() {
		return new ElasticsearchZonedDateTimeIndexFieldTypeContext( this );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Year> asYear() {
		return new ElasticsearchYearIndexFieldTypeContext( this );
	}

	@Override
	public StandardIndexFieldTypeContext<?, YearMonth> asYearMonth() {
		return new ElasticsearchYearMonthIndexFieldTypeContext( this );
	}

	@Override
	public StandardIndexFieldTypeContext<?, MonthDay> asMonthDay() {
		return new ElasticsearchMonthDayIndexFieldTypeContext( this );
	}

	@Override
	public StandardIndexFieldTypeContext<?, OffsetDateTime> asOffsetDateTime() {
		return new ElasticsearchOffsetDateTimeIndexFieldTypeContext( this );
	}

	@Override
	public StandardIndexFieldTypeContext<?, OffsetTime> asOffsetTime() {
		return new ElasticsearchOffsetTimeIndexFieldTypeContext( this );
	}

	@Override
	public StandardIndexFieldTypeContext<?, GeoPoint> asGeoPoint() {
		return new ElasticsearchGeoPointIndexFieldTypeContext( this );
	}

	@Override
	public ScaledNumberIndexFieldTypeContext<?, BigDecimal> asBigDecimal() {
		return new ElasticsearchBigDecimalIndexFieldTypeContext( this, typeDefaultsProvider );
	}

	@Override
	public ElasticsearchNativeIndexFieldTypeContext<?> asNative(String mappingJsonString) {
		return new ElasticsearchNativeIndexFieldTypeContextImpl( this, mappingJsonString );
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
