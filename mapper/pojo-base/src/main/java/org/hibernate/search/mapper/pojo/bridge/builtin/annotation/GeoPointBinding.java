/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.processor.impl.GeoPointBindingProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorRef;

/**
 * Defines a {@link GeoPoint} binding from a type or a property
 * to a {@link GeoPoint} field representing a point on earth.
 * <p>
 * If the longitude and latitude information is hosted on two different properties,
 * {@code @GeoPointBinding} must be used on the entity (class level).
 * The {@link Latitude} and {@link Longitude} annotations must mark the properties.
 * <pre><code>
 * &#064;GeoPointBinding(name="home")
 * public class User {
 *     &#064;Latitude
 *     public Double getHomeLatitude() { ... }
 *     &#064;Longitude
 *     public Double getHomeLongitude() { ... }
 * }
 * </code></pre>
 * <p>
 * Alternatively, {@code @GeoPointBinding} can be used on a type that implements {@link GeoPoint}:
 * <pre><code>
 * &#064;GeoPointBinding(name="location")
 * public class Home implements GeoPoint {
 *     &#064;Override
 *     public Double getLatitude() { ... }
 *     &#064;Override
 *     public Double getLongitude() { ... }
 * }
 * </code></pre>
 * <p>
 * ... or on a property of type {@link GeoPoint}:
 * <pre><code>
 * public class User {
 *     &#064;GeoPointBinding
 *     public GeoPoint getHome() { ... }
 * }
 * </code></pre>
 *
 * @author Nicolas Helleringer
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.TYPE })
@Documented
@Repeatable(GeoPointBinding.List.class)
@TypeMapping(processor = @TypeMappingAnnotationProcessorRef(type = GeoPointBindingProcessor.class,
		retrieval = BeanRetrieval.CONSTRUCTOR))
@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef(type = GeoPointBindingProcessor.class,
		retrieval = BeanRetrieval.CONSTRUCTOR))
public @interface GeoPointBinding {

	/**
	 * The name of the index field holding spatial information.
	 *
	 * If {@code @GeoPoint} is hosted on a property, defaults to the property name.
	 * If {@code @GeoPoint} is hosted on a class, the name must be provided.
	 *
	 * @return the field name
	 */
	String fieldName() default "";

	/**
	 * @return Returns an instance of the {@link Projectable} enum, indicating whether projections are enabled for this
	 * field. Defaults to {@code Projectable.DEFAULT}.
	 */
	Projectable projectable() default Projectable.DEFAULT;

	/**
	 * @return Returns an instance of the {@link Sortable} enum, indicating whether sorts are enabled for this
	 * field. Defaults to {@code Sortable.DEFAULT}.
	 */
	Sortable sortable() default Sortable.DEFAULT;

	/**
	 * @return The name of the marker set this spatial should look into
	 * when looking for the {@link Latitude} and {@link Longitude} markers.
	 */
	String markerSet() default "";

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.TYPE })
	@Documented
	@interface List {

		GeoPointBinding[] value();

	}


}
