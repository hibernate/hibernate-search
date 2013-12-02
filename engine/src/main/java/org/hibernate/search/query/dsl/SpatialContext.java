/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.search.query.dsl;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public interface SpatialContext extends QueryCustomization<SpatialContext> {

	//TODO score by proximity

	/**
	 * Used to create Spatial Queries on the default coordinates of
	 * an entity. This is the one to use when {@link org.hibernate.search.annotations.Spatial} is being used
	 * without defining a custom value for {@link org.hibernate.search.annotations.Spatial#name()}.
	 *
	 * @return  {@code SpatialMatchingContext} instance for continuation
	 */
	SpatialMatchingContext onDefaultCoordinates();

	/**
	 * An entity can have multiple {@link org.hibernate.search.annotations.Spatial} annotations defining
	 * different sets of coordinates.
	 * Each non-default Spatial instance has a name to identify it,
	 * use this method to pick one of these non-default coordinate fields.
	 *
	 * @param field The name of the set of coordinates to target for the query
	 *
	 * @return {@code SpatialMatchingContext} instance for continuation
	 */
	SpatialMatchingContext onCoordinates(String field);

}
