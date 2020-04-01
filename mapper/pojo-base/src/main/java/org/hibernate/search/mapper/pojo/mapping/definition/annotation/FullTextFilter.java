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

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.FullTextFilterProcessor;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.FilterFactoryRef;

/**
 * Maps an indexed type to its filter using a {@link FullTextFilter},
 * <p>
 * See the reference documentation for more information about bridges in general,
 * and filter in particular.
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(FullTextFilter.List.class)
@TypeMapping(processor = @TypeMappingAnnotationProcessorRef(type = FullTextFilterProcessor.class))
public @interface FullTextFilter {

	/**
	 * @return The name of the filter.
	 */
	String name();

	/**
	 * @return A reference to the factory filter to use.
	 * @see FilterFactoryRef
	 */
	FilterFactoryRef factory();

	/**
	 * @return A list of components in the paths, each representing a property and a way to extract its value.
	 */
	FilterParam[] params() default {};

	@Documented
	@Target({ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	@interface List {
		FullTextFilter[] value();
	}

}
