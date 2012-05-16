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

import org.hibernate.search.spatial.SpatialFieldBridge;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Extension annotation for {@code @Field} for Hibernate Search Spatial Index
 *
 * @author Nicolas Helleringer (nicolas.helleringer@novacodex.net)

 * @experimental Spatial indexing in Hibernate Search is still in its first drafts
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.METHOD, ElementType.FIELD, ElementType.TYPE })
@Documented
public @interface Spatial {
	/**
	 * @return the field name (defaults to the JavaBean property name)
	 */
	String name() default "";

	/**
	 * @return Returns an instance of the {@link Store} enum, indicating whether the value should be stored in the document.
	 *         Defaults to {@code Store.NO}
	 */
	Store store() default Store.NO;

	/**
	 * @return Returns a {@code Boost} annotation defining a float index time boost value
	 */
	Boost boost() default @Boost(value = 1.0F);

	/**
	 * @return grid mode activation status
	 */
	SpatialMode spatialMode() default SpatialMode.SIMPLE;
	
	/**
	 * @return top range grid level for spatial indexing
	 */
	int topGridLevel() default SpatialFieldBridge.DEFAULT_TOP_GRID_LEVEL;

	/**
	 * @return bottom grid level for spatial indexing
	 */
	int bottomGridLevel() default SpatialFieldBridge.DEFAULT_BOTTOM_GRID_LEVEL;
}

