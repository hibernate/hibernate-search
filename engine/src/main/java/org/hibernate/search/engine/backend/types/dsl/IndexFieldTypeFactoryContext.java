/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.types.dsl;

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

import org.hibernate.search.engine.spatial.GeoPoint;


/**
 * @author Yoann Rodiere
 */
public interface IndexFieldTypeFactoryContext {

	<F> StandardIndexFieldTypeContext<?, F> as(Class<F> inputType);

	StringIndexFieldTypeContext<?> asString();

	StandardIndexFieldTypeContext<?, Integer> asInteger();

	StandardIndexFieldTypeContext<?, Long> asLong();

	StandardIndexFieldTypeContext<?, Boolean> asBoolean();

	StandardIndexFieldTypeContext<?, Byte> asByte();

	StandardIndexFieldTypeContext<?, Short> asShort();

	StandardIndexFieldTypeContext<?, Float> asFloat();

	StandardIndexFieldTypeContext<?, Double> asDouble();

	StandardIndexFieldTypeContext<?, LocalDate> asLocalDate();

	StandardIndexFieldTypeContext<?, LocalDateTime> asLocalDateTime();

	StandardIndexFieldTypeContext<?, LocalTime> asLocalTime();

	StandardIndexFieldTypeContext<?, Instant> asInstant();

	StandardIndexFieldTypeContext<?, ZonedDateTime> asZonedDateTime();

	StandardIndexFieldTypeContext<?, Year> asYear();

	StandardIndexFieldTypeContext<?, YearMonth> asYearMonth();

	StandardIndexFieldTypeContext<?, MonthDay> asMonthDay();

	StandardIndexFieldTypeContext<?, OffsetDateTime> asOffsetDateTime();

	StandardIndexFieldTypeContext<?, OffsetTime> asOffsetTime();

	StandardIndexFieldTypeContext<?, GeoPoint> asGeoPoint();

	ScaledNumberIndexFieldTypeContext<?, BigDecimal> asBigDecimal();

	ScaledNumberIndexFieldTypeContext<?, BigInteger> asBigInteger();

	default <T> T extension(IndexFieldTypeFactoryContextExtension<T> extension) {
		return extension.extendOrFail( this );
	}

}
