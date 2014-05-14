/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
