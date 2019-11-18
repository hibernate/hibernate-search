/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A reference to a {@link PropertyMappingAnnotationProcessor}.
 * <p>
 * References can use either a name, a type, or both.
 */
@Documented
@Target({}) // Only used as a component in other annotations
@Retention(RetentionPolicy.RUNTIME)
public @interface PropertyMappingAnnotationProcessorRef {

	/**
	 * Reference a {@link PropertyMappingAnnotationProcessor} by its bean name.
	 * @return The bean name of the annotation processor.
	 */
	String name() default "";

	/**
	 * Reference a {@link PropertyMappingAnnotationProcessor} by its bean type.
	 * @return The type of the annotation processor.
	 */
	Class<? extends PropertyMappingAnnotationProcessor<?>> type() default UndefinedProcessorImplementationType.class;

	/**
	 * Class used as a marker for the default value of the {@link #type()} attribute.
	 */
	abstract class UndefinedProcessorImplementationType implements PropertyMappingAnnotationProcessor<Annotation> {
		private UndefinedProcessorImplementationType() {
		}
	}
}
