/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.annotations;

import org.hibernate.search.spatial.SpatialFieldBridgeByQuadTree;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a spatial property.
 *
 * Spatial coordinates can be indexed as latitude / longitude fields and queried
 * via range queries. This is known as the {@code SpatialMode.RANGE} approach.
 *
 * Otherwise, they can be indexed using a quad-tree index. This is known as the
 * @code SpatialMode.GRID} approach. The size of the grid can be adjusted with {@code topGridLevel}
 * and {@code bottomGridLevel}.
 *
 * For more information on which model to use, read the Hibernate Search reference documentation.
 *
 * If your longitude and latitude information are hosted on free properties,
 * Add {@code &#064;Spatial} on the entity (class-level). The {@link Latitude} and {@link Longitude}
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
 * @experimental Spatial support is still considered experimental
 * @author Nicolas Helleringer (nicolas.helleringer@novacodex.net)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.METHOD, ElementType.FIELD, ElementType.TYPE })
@Documented
public @interface Spatial {
	/**
	 * Prefix used to generate field names for a default {@link org.hibernate.search.annotations.Spatial} annotation
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

	/**
	 * @return Returns a {@link Boost} annotation defining a float index time boost value
	 */
	Boost boost() default @Boost(value = 1.0F);

	/**
	 * @return the mode used for Spatial indexing
	 */
	SpatialMode spatialMode() default SpatialMode.RANGE;

	/**
	 * @return top range quad tree level for spatial indexing
	 */
	int topQuadTreeLevel() default SpatialFieldBridgeByQuadTree.DEFAULT_TOP_QUAD_TREE_LEVEL;

	/**
	 * @return bottom quad tree level for spatial indexing
	 */
	int bottomQuadTreeLevel() default SpatialFieldBridgeByQuadTree.DEFAULT_BOTTOM_QUAD_TREE_LEVEL;
}
