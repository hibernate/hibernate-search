/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.PropertyBridgeMapping;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.PropertyBridgeRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.TypeBridgeMapping;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.TypeBridgeRef;

/**
 * Defines a {@link GeoPoint} bridge, mapping a type or a property
 * to a {@link GeoPoint} field, representing a point on earth.
 * <p>
 * If the longitude and latitude information is hosted on two different properties,
 * {@code @GeoPointBridge} must be used on the entity (class level).
 * The {@link Latitude} and {@link Longitude} annotations must mark the properties.
 * <pre><code>
 * &#064;GeoPointBridge(name="home")
 * public class User {
 *     &#064;Latitude
 *     public Double getHomeLatitude() { ... }
 *     &#064;Longitude
 *     public Double getHomeLongitude() { ... }
 * }
 * </code></pre>
 * <p>
 * Alternatively, {@code @GeoPointBridge} can be used on a type that implements {@link GeoPoint}:
 * <pre><code>
 * &#064;GeoPointBridge(name="location")
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
 *     &#064;GeoPointBridge
 *     public GeoPoint getHome() { ... }
 * }
 * </code></pre>
 *
 * @author Nicolas Helleringer
 */
@PropertyBridgeMapping(bridge = @PropertyBridgeRef(
		binderType = org.hibernate.search.mapper.pojo.bridge.builtin.spatial.impl.GeoPointBridge.Binder.class
))
@TypeBridgeMapping(bridge = @TypeBridgeRef(
		binderType = org.hibernate.search.mapper.pojo.bridge.builtin.spatial.impl.GeoPointBridge.Binder.class
))
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.METHOD, ElementType.FIELD, ElementType.TYPE })
@Documented
@Repeatable(GeoPointBridge.List.class)
public @interface GeoPointBridge {

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

	@Retention( RetentionPolicy.RUNTIME )
	@Target( { ElementType.METHOD, ElementType.FIELD, ElementType.TYPE } )
	@Documented
	@interface List {

		GeoPointBridge[] value();

	}
}