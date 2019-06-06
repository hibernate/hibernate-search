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
import org.hibernate.search.util.common.SearchException;


/**
 * The initial context when specifying the type of an index field.
 */
public interface IndexFieldTypeFactoryContext {

	/**
	 * Define a field type whose values are represented as a given type in Hibernate Search.
	 * <p>
	 * Note this method will return a "generic" context that does not offer any type-specific options.
	 * When possible, prefer the other methods such as {@link #asString()} or {@link #asInteger()}.
	 *
	 * @param valueType The type of values for this field type.
	 * @param <F> The type of values for this field type.
	 * @return A context to define the type more precisely
	 * and ultimately {@link IndexFieldTypeTerminalContext#toIndexFieldType() get the resulting type}.
	 * @throws SearchException If the given {@code inputType} is not supported.
	 */
	<F> StandardIndexFieldTypeContext<?, F> as(Class<F> valueType);

	/**
	 * Define a field type whose values are represented as a {@link String} in Hibernate Search.
	 * @return A context to define the type more precisely
	 * and ultimately {@link IndexFieldTypeTerminalContext#toIndexFieldType() get the resulting type}.
	 */
	StringIndexFieldTypeContext<?> asString();

	/**
	 * Define a field type whose values are represented as an {@link Integer} in Hibernate Search.
	 * @return A context to define the type more precisely
	 * and ultimately {@link IndexFieldTypeTerminalContext#toIndexFieldType() get the resulting type}.
	 */
	StandardIndexFieldTypeContext<?, Integer> asInteger();

	/**
	 * Define a field type whose values are represented as a {@link Long} in Hibernate Search.
	 * @return A context to define the type more precisely
	 * and ultimately {@link IndexFieldTypeTerminalContext#toIndexFieldType() get the resulting type}.
	 */
	StandardIndexFieldTypeContext<?, Long> asLong();

	/**
	 * Define a field type whose values are represented as a {@link Boolean} in Hibernate Search.
	 * @return A context to define the type more precisely
	 * and ultimately {@link IndexFieldTypeTerminalContext#toIndexFieldType() get the resulting type}.
	 */
	StandardIndexFieldTypeContext<?, Boolean> asBoolean();

	/**
	 * Define a field type whose values are represented as a {@link Byte} in Hibernate Search.
	 * @return A context to define the type more precisely
	 * and ultimately {@link IndexFieldTypeTerminalContext#toIndexFieldType() get the resulting type}.
	 */
	StandardIndexFieldTypeContext<?, Byte> asByte();

	/**
	 * Define a field type whose values are represented as a {@link Short} in Hibernate Search.
	 * @return A context to define the type more precisely
	 * and ultimately {@link IndexFieldTypeTerminalContext#toIndexFieldType() get the resulting type}.
	 */
	StandardIndexFieldTypeContext<?, Short> asShort();

	/**
	 * Define a field type whose values are represented as a {@link Float} in Hibernate Search.
	 * @return A context to define the type more precisely
	 * and ultimately {@link IndexFieldTypeTerminalContext#toIndexFieldType() get the resulting type}.
	 */
	StandardIndexFieldTypeContext<?, Float> asFloat();

	/**
	 * Define a field type whose values are represented as a {@link Double} in Hibernate Search.
	 * @return A context to define the type more precisely
	 * and ultimately {@link IndexFieldTypeTerminalContext#toIndexFieldType() get the resulting type}.
	 */
	StandardIndexFieldTypeContext<?, Double> asDouble();

	/**
	 * Define a field type whose values are represented as a {@link LocalDate} in Hibernate Search.
	 * @return A context to define the type more precisely
	 * and ultimately {@link IndexFieldTypeTerminalContext#toIndexFieldType() get the resulting type}.
	 */
	StandardIndexFieldTypeContext<?, LocalDate> asLocalDate();

	/**
	 * Define a field type whose values are represented as a {@link LocalDateTime} in Hibernate Search.
	 * @return A context to define the type more precisely
	 * and ultimately {@link IndexFieldTypeTerminalContext#toIndexFieldType() get the resulting type}.
	 */
	StandardIndexFieldTypeContext<?, LocalDateTime> asLocalDateTime();

	/**
	 * Define a field type whose values are represented as a {@link LocalTime} in Hibernate Search.
	 * @return A context to define the type more precisely
	 * and ultimately {@link IndexFieldTypeTerminalContext#toIndexFieldType() get the resulting type}.
	 */
	StandardIndexFieldTypeContext<?, LocalTime> asLocalTime();

	/**
	 * Define a field type whose values are represented as an {@link Instant} in Hibernate Search.
	 * @return A context to define the type more precisely
	 * and ultimately {@link IndexFieldTypeTerminalContext#toIndexFieldType() get the resulting type}.
	 */
	StandardIndexFieldTypeContext<?, Instant> asInstant();

	/**
	 * Define a field type whose values are represented as a {@link ZonedDateTime} in Hibernate Search.
	 * @return A context to define the type more precisely
	 * and ultimately {@link IndexFieldTypeTerminalContext#toIndexFieldType() get the resulting type}.
	 */
	StandardIndexFieldTypeContext<?, ZonedDateTime> asZonedDateTime();

	/**
	 * Define a field type whose values are represented as a {@link Year} in Hibernate Search.
	 * @return A context to define the type more precisely
	 * and ultimately {@link IndexFieldTypeTerminalContext#toIndexFieldType() get the resulting type}.
	 */
	StandardIndexFieldTypeContext<?, Year> asYear();

	/**
	 * Define a field type whose values are represented as a {@link YearMonth} in Hibernate Search.
	 * @return A context to define the type more precisely
	 * and ultimately {@link IndexFieldTypeTerminalContext#toIndexFieldType() get the resulting type}.
	 */
	StandardIndexFieldTypeContext<?, YearMonth> asYearMonth();

	/**
	 * Define a field type whose values are represented as a {@link MonthDay} in Hibernate Search.
	 * @return A context to define the type more precisely
	 * and ultimately {@link IndexFieldTypeTerminalContext#toIndexFieldType() get the resulting type}.
	 */
	StandardIndexFieldTypeContext<?, MonthDay> asMonthDay();

	/**
	 * Define a field type whose values are represented as an {@link OffsetDateTime} in Hibernate Search.
	 * @return A context to define the type more precisely
	 * and ultimately {@link IndexFieldTypeTerminalContext#toIndexFieldType() get the resulting type}.
	 */
	StandardIndexFieldTypeContext<?, OffsetDateTime> asOffsetDateTime();

	/**
	 * Define a field type whose values are represented as an {@link OffsetTime} in Hibernate Search.
	 * @return A context to define the type more precisely
	 * and ultimately {@link IndexFieldTypeTerminalContext#toIndexFieldType() get the resulting type}.
	 */
	StandardIndexFieldTypeContext<?, OffsetTime> asOffsetTime();

	/**
	 * Define a field type whose values are represented as a {@link GeoPoint} in Hibernate Search.
	 * @return A context to define the type more precisely
	 * and ultimately {@link IndexFieldTypeTerminalContext#toIndexFieldType() get the resulting type}.
	 */
	StandardIndexFieldTypeContext<?, GeoPoint> asGeoPoint();

	/**
	 * Define a field type whose values are represented as a {@link BigDecimal} in Hibernate Search.
	 * @return A context to define the type more precisely
	 * and ultimately {@link IndexFieldTypeTerminalContext#toIndexFieldType() get the resulting type}.
	 */
	ScaledNumberIndexFieldTypeContext<?, BigDecimal> asBigDecimal();

	/**
	 * Define a field type whose values are represented as a {@link BigInteger} in Hibernate Search.
	 * @return A context to define the type more precisely
	 * and ultimately {@link IndexFieldTypeTerminalContext#toIndexFieldType() get the resulting type}.
	 */
	ScaledNumberIndexFieldTypeContext<?, BigInteger> asBigInteger();

	/**
	 * Extend the current context with the given extension,
	 * resulting in an extended context offering more field types.
	 *
	 * @param extension The extension to apply.
	 * @param <T> The type of context provided by the extension.
	 * @return The extended context.
	 * @throws SearchException If the extension cannot be applied (wrong underlying technology, ...).
	 */
	default <T> T extension(IndexFieldTypeFactoryContextExtension<T> extension) {
		return extension.extendOrFail( this );
	}

}
