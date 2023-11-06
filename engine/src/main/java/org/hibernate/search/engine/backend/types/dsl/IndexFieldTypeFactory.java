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
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A factory for types of index fields.
 */
public interface IndexFieldTypeFactory {

	/**
	 * Define a field type whose values are represented as a given type in Hibernate Search.
	 * <p>
	 * Note this method will return a "generic" DSL step that does not offer any type-specific options.
	 * When possible, prefer the other methods such as {@link #asString()} or {@link #asInteger()}.
	 *
	 * @param valueType The type of values for this field type.
	 * @param <F> The type of values for this field type.
	 * @return A DSL step where the index field type can be defined in more details.
	 * @throws SearchException If the given {@code inputType} is not supported.
	 */
	<F> StandardIndexFieldTypeOptionsStep<?, F> as(Class<F> valueType);

	/**
	 * Define a vector field type whose values are represented as a given type in Hibernate Search.
	 * <p>
	 * When possible, prefer the other methods such as {@link #asByteVector()} or {@link #asFloatVector()}
	 * to avoid unnecessary type checks.
	 *
	 * @param valueType The type of values for this field type. Should be an array type like {@code byte[]} or {@code float[]}.
	 * @return A DSL step where the index vector field type can be defined in more details.
	 * @param <F> The type of values for this field type.
	 */
	<F> VectorFieldTypeOptionsStep<?, F> asVector(Class<F> valueType);

	/**
	 * Define a field type whose values are represented as a {@link String} in Hibernate Search.
	 * @return A DSL step where the index field type can be defined in more details.
	 */
	StringIndexFieldTypeOptionsStep<?> asString();

	/**
	 * Define a field type whose values are represented as an {@link Integer} in Hibernate Search.
	 * @return A DSL step where the index field type can be defined in more details.
	 */
	StandardIndexFieldTypeOptionsStep<?, Integer> asInteger();

	/**
	 * Define a field type whose values are represented as a {@link Long} in Hibernate Search.
	 * @return A DSL step where the index field type can be defined in more details.
	 */
	StandardIndexFieldTypeOptionsStep<?, Long> asLong();

	/**
	 * Define a field type whose values are represented as a {@link Boolean} in Hibernate Search.
	 * @return A DSL step where the index field type can be defined in more details.
	 */
	StandardIndexFieldTypeOptionsStep<?, Boolean> asBoolean();

	/**
	 * Define a field type whose values are represented as a {@link Byte} in Hibernate Search.
	 * @return A DSL step where the index field type can be defined in more details.
	 */
	StandardIndexFieldTypeOptionsStep<?, Byte> asByte();

	/**
	 * Define a field type whose values are represented as a {@link Short} in Hibernate Search.
	 * @return A DSL step where the index field type can be defined in more details.
	 */
	StandardIndexFieldTypeOptionsStep<?, Short> asShort();

	/**
	 * Define a field type whose values are represented as a {@link Float} in Hibernate Search.
	 * @return A DSL step where the index field type can be defined in more details.
	 */
	StandardIndexFieldTypeOptionsStep<?, Float> asFloat();

	/**
	 * Define a field type whose values are represented as a {@link Double} in Hibernate Search.
	 * @return A DSL step where the index field type can be defined in more details.
	 */
	StandardIndexFieldTypeOptionsStep<?, Double> asDouble();

	/**
	 * Define a field type whose values are represented as a {@link LocalDate} in Hibernate Search.
	 * @return A DSL step where the index field type can be defined in more details.
	 */
	StandardIndexFieldTypeOptionsStep<?, LocalDate> asLocalDate();

	/**
	 * Define a field type whose values are represented as a {@link LocalDateTime} in Hibernate Search.
	 * @return A DSL step where the index field type can be defined in more details.
	 */
	StandardIndexFieldTypeOptionsStep<?, LocalDateTime> asLocalDateTime();

	/**
	 * Define a field type whose values are represented as a {@link LocalTime} in Hibernate Search.
	 * @return A DSL step where the index field type can be defined in more details.
	 */
	StandardIndexFieldTypeOptionsStep<?, LocalTime> asLocalTime();

	/**
	 * Define a field type whose values are represented as an {@link Instant} in Hibernate Search.
	 * @return A DSL step where the index field type can be defined in more details.
	 */
	StandardIndexFieldTypeOptionsStep<?, Instant> asInstant();

	/**
	 * Define a field type whose values are represented as a {@link ZonedDateTime} in Hibernate Search.
	 * @return A DSL step where the index field type can be defined in more details.
	 */
	StandardIndexFieldTypeOptionsStep<?, ZonedDateTime> asZonedDateTime();

	/**
	 * Define a field type whose values are represented as a {@link Year} in Hibernate Search.
	 * @return A DSL step where the index field type can be defined in more details.
	 */
	StandardIndexFieldTypeOptionsStep<?, Year> asYear();

	/**
	 * Define a field type whose values are represented as a {@link YearMonth} in Hibernate Search.
	 * @return A DSL step where the index field type can be defined in more details.
	 */
	StandardIndexFieldTypeOptionsStep<?, YearMonth> asYearMonth();

	/**
	 * Define a field type whose values are represented as a {@link MonthDay} in Hibernate Search.
	 * @return A DSL step where the index field type can be defined in more details.
	 */
	StandardIndexFieldTypeOptionsStep<?, MonthDay> asMonthDay();

	/**
	 * Define a field type whose values are represented as an {@link OffsetDateTime} in Hibernate Search.
	 * @return A DSL step where the index field type can be defined in more details.
	 */
	StandardIndexFieldTypeOptionsStep<?, OffsetDateTime> asOffsetDateTime();

	/**
	 * Define a field type whose values are represented as an {@link OffsetTime} in Hibernate Search.
	 * @return A DSL step where the index field type can be defined in more details.
	 */
	StandardIndexFieldTypeOptionsStep<?, OffsetTime> asOffsetTime();

	/**
	 * Define a field type whose values are represented as a {@link GeoPoint} in Hibernate Search.
	 * @return A DSL step where the index field type can be defined in more details.
	 */
	StandardIndexFieldTypeOptionsStep<?, GeoPoint> asGeoPoint();

	/**
	 * Define a field type whose values are represented as a {@link BigDecimal} in Hibernate Search.
	 * @return A DSL step where the index field type can be defined in more details.
	 */
	ScaledNumberIndexFieldTypeOptionsStep<?, BigDecimal> asBigDecimal();

	/**
	 * Define a field type whose values are represented as a {@link BigInteger} in Hibernate Search.
	 * @return A DSL step where the index field type can be defined in more details.
	 */
	ScaledNumberIndexFieldTypeOptionsStep<?, BigInteger> asBigInteger();


	/**
	 * Define a field type intended for use in vector search
	 * and whose values are represented as a {@code byte[]} in Hibernate Search.
	 * @return A DSL step where the index field type can be defined in more details.
	 */
	@Incubating
	VectorFieldTypeOptionsStep<?, byte[]> asByteVector();

	/**
	 * Define a field type intended for use in vector search
	 * and whose values are represented as a {@code float[]} in Hibernate Search.
	 * @return A DSL step where the index field type can be defined in more details.
	 */
	@Incubating
	VectorFieldTypeOptionsStep<?, float[]> asFloatVector();

	/**
	 * Extend the current factory with the given extension,
	 * resulting in an extended factory offering more field types.
	 *
	 * @param extension The extension to apply.
	 * @param <T> The type of factory provided by the extension.
	 * @return The extended factory.
	 * @throws SearchException If the extension cannot be applied (wrong underlying technology, ...).
	 */
	default <T> T extension(IndexFieldTypeFactoryExtension<T> extension) {
		return extension.extendOrFail( this );
	}

}
