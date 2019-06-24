/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

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

import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.backend.lucene.types.converter.LuceneFieldContributor;
import org.hibernate.search.backend.lucene.types.converter.LuceneFieldValueExtractor;
import org.hibernate.search.backend.lucene.types.dsl.LuceneIndexFieldTypeFactoryContext;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFinalStep;
import org.hibernate.search.engine.backend.types.dsl.ScaledNumberIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;



public class LuceneIndexFieldTypeFactoryContextImpl
		implements LuceneIndexFieldTypeFactoryContext, LuceneIndexFieldTypeBuildContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final EventContext eventContext;
	private final LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry;
	private final IndexFieldTypeDefaultsProvider typeDefaultsProvider;

	public LuceneIndexFieldTypeFactoryContextImpl(EventContext eventContext,
			LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry, IndexFieldTypeDefaultsProvider typeDefaultsProvider) {
		this.eventContext = eventContext;
		this.analysisDefinitionRegistry = analysisDefinitionRegistry;
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
		return new LuceneStringIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, Integer> asInteger() {
		return new LuceneIntegerIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, Long> asLong() {
		return new LuceneLongIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, Boolean> asBoolean() {
		return new LuceneBooleanIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, Byte> asByte() {
		return new LuceneByteIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, Short> asShort() {
		return new LuceneShortIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, Float> asFloat() {
		return new LuceneFloatIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, Double> asDouble() {
		return new LuceneDoubleIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, LocalDate> asLocalDate() {
		return new LuceneLocalDateIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, LocalDateTime> asLocalDateTime() {
		return new LuceneLocalDateTimeIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, LocalTime> asLocalTime() {
		return new LuceneLocalTimeIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, Instant> asInstant() {
		return new LuceneInstantIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, ZonedDateTime> asZonedDateTime() {
		return new LuceneZonedDateTimeIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, Year> asYear() {
		return new LuceneYearIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, YearMonth> asYearMonth() {
		return new LuceneYearMonthIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, MonthDay> asMonthDay() {
		return new LuceneMonthDayIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, OffsetDateTime> asOffsetDateTime() {
		return new LuceneOffsetDateTimeIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, OffsetTime> asOffsetTime() {
		return new LuceneOffsetTimeIndexFieldTypeOptionsStep( this );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, GeoPoint> asGeoPoint() {
		return new LuceneGeoPointIndexFieldTypeOptionsStep( this );
	}

	@Override
	public ScaledNumberIndexFieldTypeOptionsStep<?, BigDecimal> asBigDecimal() {
		return new LuceneBigDecimalIndexFieldTypeOptionsStep( this, typeDefaultsProvider );
	}

	@Override
	public ScaledNumberIndexFieldTypeOptionsStep<?, BigInteger> asBigInteger() {
		return new LuceneBigIntegerIndexFieldTypeOptionsStep( this, typeDefaultsProvider );
	}

	@Override
	public <F> IndexFieldTypeFinalStep<F> asNative(Class<F> indexFieldType,
			LuceneFieldContributor<F> fieldContributor,
			LuceneFieldValueExtractor<F> fieldValueExtractor) {
		return new LuceneNativeIndexFieldTypeFinalStep<>(
				indexFieldType, fieldContributor, fieldValueExtractor
		);
	}

	@Override
	public EventContext getEventContext() {
		return eventContext;
	}

	@Override
	public LuceneAnalysisDefinitionRegistry getAnalysisDefinitionRegistry() {
		return analysisDefinitionRegistry;
	}
}
