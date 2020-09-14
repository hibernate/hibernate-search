/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;

/**
 * Defines the temporal resolution and encoding type of a {@code Calendar} field.
 *
 * <p>
 * <b>Note:</b> Dates are encoded in the GMT/UTC time zone.
 * </p>
 *
 * @author Amin Mohammed-Coleman
 * @deprecated {@link DateBridge}/{@link CalendarBridge} are no longer available in Hibernate Search 6.
 * If you cannot move your properties to Java 8 date/time types, implement your own bridge and apply it with {@link GenericField#valueBridge()}.
 * If you can move your properties to Java 8 date/time types,
 * do so, remove this annotation and replace {@link Field} with {@link GenericField}.
 * Then, either use a Java type with the appropriate resolution ({@link java.time.LocalDate}, ...) and {@code match} predicates,
 * or use a type with more resolution than necessary but
 * rely on range predicates (e.g. {@code f.range().field("myField").between(LocalDate.now().atStartOfDay(), LocalDate.now().plusDays(1).atStartOfDay())}
 * for a "day" resolution on a {@code LocalDateTime} field).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
@Documented
@Deprecated
public @interface CalendarBridge {
	/**
	 * @return the resolution for the annotated {@code Calendar} instance. The date of the calendar will be rounded to the specified resolution.
	 */
	Resolution resolution();

}
