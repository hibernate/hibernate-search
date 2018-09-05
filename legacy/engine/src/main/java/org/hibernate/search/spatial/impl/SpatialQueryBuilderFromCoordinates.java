/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spatial.impl;

import java.util.List;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.spatial.SpatialFieldBridgeByHash;

/**
 * The SpatialQueryBuilder holds builder methods for Hash, Distance and Spatial (Hash+Distance) queries
 *
 * @author Nicolas Helleringer
 */
public abstract class SpatialQueryBuilderFromCoordinates {

	/**
	 * Returns a Lucene query to match documents by distance to a center,
	 * relying only on spatial hashes.
	 *
	 * @param center center of the search discus
	 * @param radius distance max to center in km
	 * @param fieldName name of the Lucene Field implementing Coordinates
	 * @return Lucene Query to be used in a search
	 *
	 * @see org.apache.lucene.search.Query
	 * @see org.hibernate.search.spatial.Coordinates
	 */
	public static Query buildSpatialHashQuery(Coordinates center, double radius, String fieldName) {
		int bestSpatialHashLevel = SpatialHelper.findBestSpatialHashLevelForSearchRange( 2.0d * radius );
		if ( bestSpatialHashLevel > SpatialFieldBridgeByHash.DEFAULT_BOTTOM_SPATIAL_HASH_LEVEL ) {
			bestSpatialHashLevel = SpatialFieldBridgeByHash.DEFAULT_BOTTOM_SPATIAL_HASH_LEVEL;
		}
		List<String> spatialHashCellsIds = SpatialHelper.getSpatialHashCellsIds( center, radius, bestSpatialHashLevel );
		return new SpatialHashQuery( spatialHashCellsIds, SpatialHelper.formatFieldName( bestSpatialHashLevel, fieldName ) );
	}

	/**
	 * Returns a Lucene query to match documents by distance to a center.
	 *
	 * @param center center of the search discus
	 * @param radius distance max to center in km
	 * @param coordinatesField name of the Lucene Field implementing Coordinates
	 * @param approximationQuery an approximation of the distance query
	 * (i.e. a query returning all the results returned by the distance query,
	 * but also some false positives).
	 * WARNING: when passing {@code null}, every single document will be scanned
	 * (time/resource consuming!)
	 * @return Lucene Query to be used in a search
	 *
	 * @see org.apache.lucene.search.Query
	 * @see org.hibernate.search.spatial.Coordinates
	 */
	public static Query buildDistanceQuery(Query approximationQuery, Coordinates center, double radius, String coordinatesField) {
		return new DistanceQuery( approximationQuery, center, radius, coordinatesField );
	}

	/**
	 * Returns a Lucene query to match documents by distance to a center.
	 *
	 * @param center center of the search discus
	 * @param radius distance max to center in km
	 * @param latitudeField name of the Lucene Field hosting latitude
	 * @param longitudeField name of the Lucene Field hosting longitude
	 * @param approximationQuery an approximation of the distance query
	 * (i.e. a query returning all the results returned by the distance query,
	 * but also some false positives).
	 * WARNING: when passing {@code null}, every single document will be scanned
	 * (time/resource consuming!)
	 * @return Lucene Query to be used in a search
	 *
	 * @see org.apache.lucene.search.Query
	 * @see org.hibernate.search.spatial.Coordinates
	 */
	public static Query buildDistanceQuery(Query approximationQuery, Coordinates center, double radius, String latitudeField, String longitudeField) {
		return new DistanceQuery( approximationQuery, center, radius, latitudeField, longitudeField );
	}

	/**
	 * Returns a Lucene Query searching directly by computing distance against
	 * all docs in the index (costly !)
	 *
	 * @param center center of the search discus
	 * @param radius distance max to center in km
	 * @param fieldName name of the Lucene Field implementing Coordinates
	 * @return Lucene Query to be used in a search
	 *
	 * @see Query
	 * @see org.hibernate.search.spatial.Coordinates
	 */
	public static Query buildDistanceQuery(Coordinates center, double radius, String fieldName) {
		return buildDistanceQuery( null, center, radius, fieldName );
	}

	/**
	 * Returns a Lucene query to match documents by distance to a center,
	 * relying first on spatial hash to approximate the result, and then on a more
	 * precise (but more costly) {@link DistanceQuery}.
	 *
	 * @param center center of the search discus
	 * @param radius distance max to center in km
	 * @param fieldName name of the Lucene Field implementing Coordinates
	 * @return Lucene Query to be used in a search
	 *
	 * @see Query
	 * @see org.hibernate.search.spatial.Coordinates
	 */
	public static Query buildSpatialQueryByHash(Coordinates center, double radius, String fieldName) {
		return buildDistanceQuery(
				buildSpatialHashQuery( center, radius, fieldName ),
				center,
				radius,
				fieldName
		);
	}

	/**
	 * Returns a Lucene Query which rely on double numeric range query
	 * on Latitude / Longitude
	 *
	 * @param centerCoordinates center of the search discus
	 * @param radius distance max to center in km
	 * @param fieldName name of the Lucene Field implementing Coordinates
	 * @return Lucene Query to be used in a search
	 *
	 * @see Query
	 * @see org.hibernate.search.spatial.Coordinates
	 */
	public static Query buildSpatialQueryByRange(Coordinates centerCoordinates, double radius, String fieldName) {
		Point center = Point.fromCoordinates( centerCoordinates );
		Rectangle boundingBox = Rectangle.fromBoundingCircle( center, radius );

		String latitudeFieldName = fieldName + "_HSSI_Latitude";
		String longitudeFieldName = fieldName + "_HSSI_Longitude";

		Query latQuery = NumericRangeQuery.newDoubleRange(
				latitudeFieldName, boundingBox.getLowerLeft().getLatitude(),
				boundingBox.getUpperRight().getLatitude(), true, true
		);

		Query longQuery = null;
		if ( boundingBox.getLowerLeft().getLongitude() <= boundingBox.getUpperRight().getLongitude() ) {
			longQuery = NumericRangeQuery.newDoubleRange( longitudeFieldName, boundingBox.getLowerLeft().getLongitude(),
					boundingBox.getUpperRight().getLongitude(), true, true );
		}
		else {
			longQuery = new BooleanQuery.Builder()
					.add( NumericRangeQuery.newDoubleRange( longitudeFieldName, boundingBox.getLowerLeft().getLongitude(),
						180.0, true, true ), BooleanClause.Occur.SHOULD )
					.add( NumericRangeQuery.newDoubleRange( longitudeFieldName, -180.0,
						boundingBox.getUpperRight().getLongitude(), true, true ), BooleanClause.Occur.SHOULD )
					.build();
		}

		BooleanQuery boxQuery = new BooleanQuery.Builder()
				.add( latQuery, BooleanClause.Occur.FILTER )
				.add( longQuery, BooleanClause.Occur.FILTER )
				.build();

		return buildDistanceQuery(
				boxQuery,
				center,
				radius,
				latitudeFieldName,
				longitudeFieldName
		);
	}
}
