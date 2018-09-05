/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spatial;

import org.apache.lucene.search.Query;
import org.hibernate.search.spatial.impl.Point;
import org.hibernate.search.spatial.impl.SpatialQueryBuilderFromCoordinates;

/**
 * The SpatialQueryBuilder hold builder methods for Hash, Distance and Spatial (Hash+Distance) filters
 * and queries
 *
 * @author Nicolas Helleringer
 */
public abstract class SpatialQueryBuilder {

	/**
	 * Returns a Lucene Query which relies on Hibernate Search Spatial
	 * spatial hash indexation to find candidate documents and filter its results
	 * in radius range by a DistanceFilter
	 *
	 * @param latitude WGS84 latitude of the center of the search
	 * @param longitude WGS84 longitude of the center of the search
	 * @param radius distance max to center in km
	 * @param fieldName name of the Lucene Field implementing Coordinates
	 *
	 * @return Lucene Query to be used in a search
	 *
	 * @see	Query
	 * @see	Coordinates
	 */
	public static Query buildSpatialQueryByHash(double latitude, double longitude, double radius, String fieldName) {
		return SpatialQueryBuilderFromCoordinates
				.buildSpatialQueryByHash(
						Point.fromDegrees( latitude, longitude ),
						radius,
						fieldName );
	}

	/**
	 * Returns a Lucene Query which relies on Hibernate Search Spatial
	 * double range indexation to filter document at radius and filter its results
	 * by a fine DistanceFilter
	 *
	 * @param latitude WGS84 latitude of the center of the search
	 * @param longitude WGS84 longitude of the center of the search
	 * @param radius distance max to center in km
	 * @param fieldName name of the Lucene Field implementing Coordinates
	 *
	 * @return Lucene Query to be used in a search
	 *
	 * @see	Query
	 * @see	Coordinates
	 */
	public static Query buildSpatialQueryByRange(double latitude, double longitude, double radius, String fieldName) {
		return SpatialQueryBuilderFromCoordinates.buildSpatialQueryByRange(
				Point.fromDegrees( latitude, longitude ),
				radius,
				fieldName
		);
	}
}
