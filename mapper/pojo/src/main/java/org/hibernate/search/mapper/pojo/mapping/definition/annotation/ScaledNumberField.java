/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;

@Documented
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ScaledNumberField.List.class)
public @interface ScaledNumberField {

	/**
	 * @return The name of the index field.
	 */
	String name() default "";

	/**
	 * @return Whether projections are enabled for this field.
	 * @see GenericField#projectable()
	 */
	Projectable projectable() default Projectable.DEFAULT;

	/**
	 * @return Whether this field should be sortable.
	 * @see GenericField#sortable()
	 */
	Sortable sortable() default Sortable.DEFAULT;

	/**
	 * @return An optional value to replace any null value.
	 */
	String indexNullAs() default AnnotationDefaultValues.DO_NOT_INDEX_NULL;

	/**
	 * @return The decimal scale to apply to the numeric value
	 */
	int decimalScale() default AnnotationDefaultValues.DEFAULT_DECIMAL_SCALE;

	/**
	 * @return A reference to the value bridge to use for this field.
	 * @see ValueBridgeRef
	 */
	ValueBridgeRef valueBridge() default @ValueBridgeRef;

	/**
	 * @return An array of reference to container value extractor implementation classes.
	 * @see GenericField#extractors()
	 */
	ContainerExtractorRef[] extractors() default @ContainerExtractorRef;

	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD })
	@Retention(RetentionPolicy.RUNTIME)
	@interface List {
		ScaledNumberField[] value();
	}
}
