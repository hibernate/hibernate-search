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

import org.hibernate.search.annotations.impl.LatitudeAnnotationProcessor;
import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorRef;

/**
 * Mark the property hosting the latitude of a specific spatial coordinate.
 * The property must be of type {@code Double} (or its native version).
 *
 * @hsearch.experimental Spatial support is still considered experimental
 * @author Nicolas Helleringer
 * @deprecated Use Hibernate Search 6's {@link org.hibernate.search.mapper.pojo.bridge.builtin.annotation.Latitude}
 * instead. See also the javadoc of {@link Spatial}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
@Documented
@Deprecated
@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef(type = LatitudeAnnotationProcessor.class,
		retrieval = BeanRetrieval.CONSTRUCTOR))
public @interface Latitude {
	/**
	 * @return the name of the spatial field (defined in @Spatial.name)
	 */
	String of() default "";
}
