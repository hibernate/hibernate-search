/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.processor.impl.LatitudeProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorRef;

/**
 * Mark the property hosting the latitude of a specific spatial coordinate.
 * The property must be of type {@code Double} or {@code double}.
 *
 * @author Nicolas Helleringer
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.METHOD, ElementType.FIELD } )
@Documented
@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef(type = LatitudeProcessor.class))
public @interface Latitude {

	/**
	 * @return The name of the marker set this marker belongs to.
	 * Set it to the value of {@link GeoPointBinding#markerSet()}
	 * so that the bridge detects this marker.
	 */
	String markerSet() default "";

}
