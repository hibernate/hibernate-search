/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.annotations.impl.SpatialAnnotationProcessor;
import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.GeoPointBinding;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorRef;

/**
 * Defines a spatial property.
 *
 * Spatial coordinates can be indexed as latitude / longitude fields and queried
 * via range queries. This is known as the {@code SpatialMode.RANGE} approach.
 *
 * Otherwise, they can be indexed using a spatial hash index. This is known as the
 * {@code SpatialMode.HASH} approach. The size of the grid can be adjusted with {@code topSpatialHashLevel}
 * and {@code bottomSpatialHashLevel}.
 *
 * For more information on which model to use, read the Hibernate Search reference documentation.
 *
 * If your longitude and latitude information are hosted on free properties,
 * Add {@code @Spatial} on the entity (class-level). The {@link Latitude} and {@link Longitude}
 * annotations must mark the properties.
 *
 * <pre>
 * &#064;Entity
 * &#064;Spatial(name="home")
 * public class User {
 *     &#064;Latitude(of="home")
 *     public Double getHomeLatitude() { ... }
 *     &#064;Longitude(of="home")
 *     public Double getHomeLongitude() { ... }
 * }
 * </pre>
 *
 * Alternatively, you can put the latitude / longitude information in a property of
 * type {@link org.hibernate.search.spatial.Coordinates}.
 *
 * <pre>
  * &#064;Entity
  * public class User {
  *     &#064;Spatial
  *     public Coordinates getHome() { ... }
  * }
  * </pre>
 *
 * @hsearch.experimental Spatial support is still considered experimental
 * @author Nicolas Helleringer
 * @deprecated If the latitude/longitude of the element annotated with {@link Spatial} are mutable,
 * annotate these properties annotated with {@link Latitude}/{@link Longitude},
 * and use {@link GeoPointBinding} instead of {@link Spatial}.
 * If the latitude/longitude of the element annotated with {@link Spatial} are immutable,
 * you can alternatively implement {@link GeoPoint} instead
 * of {@link org.hibernate.search.spatial.Coordinates} and simply use {@link GenericField} on properties of this type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.TYPE })
@Documented
@Deprecated
@Repeatable(Spatials.class)
@TypeMapping(processor = @TypeMappingAnnotationProcessorRef(type = SpatialAnnotationProcessor.class,
		retrieval = BeanRetrieval.CONSTRUCTOR))
@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef(type = SpatialAnnotationProcessor.class,
		retrieval = BeanRetrieval.CONSTRUCTOR))
public @interface Spatial {
	/**
	 * Prefix used to generate field names for a default {@link Spatial} annotation
	 */
	String COORDINATES_DEFAULT_FIELD = "_hibernate_default_coordinates";

	/**
	 * The name of the field prefix where spatial index
	 * information is stored in a Lucene document.
	 *
	 * If {@code @Spatial} is hosted on a property, defaults to the property name.
	 *
	 * @return the field name
	 */
	String name() default "";

	/**
	 * @return Returns an instance of the {@link Store} enum, indicating whether the value should be stored in the document.
	 *         Defaults to {@code Store.NO}
	 */
	Store store() default Store.NO;

}
