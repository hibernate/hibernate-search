/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spatial.impl;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.FilteredQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;

import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.spatial.SpatialFieldBridgeByHash;

import java.util.List;

/**
 * The SpatialQueryBuilder holds builder methods for Hash, Distance and Spatial (Hash+Distance) filters
 * and queries
 *
 * @author Nicolas Helleringer
 */
public abstract class SpatialQueryBuilderFromCoordinates {

	/**
	 * Returns a Lucene filter which rely on Hibernate Search Spatial
	 * spatial hash indexation to filter document at radius
	 *
	 * @param center center of the search discus
	 * @param radius distance max to center in km
	 * @param fieldName name of the Lucene Field implementing Coordinates
	 * @return Lucene filter to be used in a Query
	 * @see org.apache.lucene.search.Query
	 * @see org.hibernate.search.spatial.Coordinates
	 * @see org.apache.lucene.search.Filter
	 */
	public static Filter buildSpatialHashFilter(Coordinates center, double radius, String fieldName) {
		int bestSpatialHashLevel = SpatialHelper.findBestSpatialHashLevelForSearchRange( 2.0d * radius );
		if ( bestSpatialHashLevel > SpatialFieldBridgeByHash.DEFAULT_BOTTOM_SPATIAL_HASH_LEVEL ) {
			bestSpatialHashLevel = SpatialFieldBridgeByHash.DEFAULT_BOTTOM_SPATIAL_HASH_LEVEL;
		}
		List<String> spatialHashCellsIds = SpatialHelper.getSpatialHashCellsIds( center, radius, bestSpatialHashLevel );
		return new SpatialHashFilter( spatialHashCellsIds, SpatialHelper.formatFieldName( bestSpatialHashLevel, fieldName ) );
	}

	/**
	 * Returns a Lucene filter to fine filter document by distance
	 *
	 * @param center center of the search discus
	 * @param radius distance max to center in km
	 * @param coordinatesField name of the Lucene Field implementing Coordinates
	 * @return Lucene filter to be used in a Query
	 * @param previousFilter	preceding filter in filter chain
	 * Warning if passed null DistanceFilter constructor use a
	 * filter wrapped match all query (time/resource consuming !)
	 * @see org.apache.lucene.search.Query
	 * @see org.hibernate.search.spatial.Coordinates
	 * @see DistanceFilter
	 * @see Filter
	 */
	public static Filter buildDistanceFilter(Filter previousFilter, Coordinates center, double radius, String coordinatesField) {
		return new DistanceFilter( previousFilter, center, radius, coordinatesField );
	}

	/**
	 * Returns a Lucene filter to fine filter document by distance
	 *
	 * @param center center of the search discus
	 * @param radius distance max to center in km
	 * @param latitudeField name of the Lucene Field hosting latitude
	 * @param longitudeField name of the Lucene Field hosting longitude
	 * @return Lucene filter to be used in a Query
	 * @param previousFilter	preceding filter in filter chain
	 * Warning if passed null DistanceFilter constructor use a
	 * filter wrapped match all query (time/ressource consuming !)
	 * @see org.apache.lucene.search.Query
	 * @see org.hibernate.search.spatial.Coordinates
	 * @see DistanceFilter
	 * @see Filter
	 */
	public static Filter buildDistanceFilter(Filter previousFilter, Coordinates center, double radius, String latitudeField, String longitudeField) {
		return new DistanceFilter( previousFilter, center, radius, latitudeField, longitudeField );
	}

	/**
	 * Returns a Lucene Query which rely on Hibernate Search Spatial
	 * spatial hash indexation to filter document at radius by wrapping a
	 * SpatialHashFilter
	 *
	 * @param center center of the search discus
	 * @param radius distance max to center in km
	 * @param fieldName name of the Lucene Field implementing Coordinates
	 * @return Lucene Query to be used in a search
	 * @see org.apache.lucene.search.Query
	 * @see org.hibernate.search.spatial.Coordinates
	 */
	public static Query buildSpatialHashQuery(Coordinates center, double radius, String fieldName) {
		return new FilteredQuery( new MatchAllDocsQuery(), buildSpatialHashFilter( center, radius, fieldName ) );
	}

	/**
	 * Returns a Lucene Query searching directly by computing distance against
	 * all docs in the index (costly !)
	 *
	 * @param center center of the search discus
	 * @param radius distance max to center in km
	 * @param fieldName name of the Lucene Field implementing Coordinates
	 * @return Lucene Query to be used in a search
	 * @see Query
	 * @see org.hibernate.search.spatial.Coordinates
	 */
	public static Query buildDistanceQuery(Coordinates center, double radius, String fieldName) {
		Filter allFilter = new QueryWrapperFilter( new MatchAllDocsQuery() );
		return new FilteredQuery( new MatchAllDocsQuery(), buildDistanceFilter( allFilter, center, radius, fieldName ) );
	}

	/**
	 * Returns a Lucene Query which relies on Hibernate Search Spatial
	 * spatial hash indexation to filter documents at radius and filter its results
	 * by a fine DistanceFilter
	 *
	 * @param center center of the search discus
	 * @param radius distance max to center in km
	 * @param fieldName name of the Lucene Field implementing Coordinates
	 * @return Lucene Query to be used in a search
	 * @see Query
	 * @see org.hibernate.search.spatial.Coordinates
	 */
	public static Query buildSpatialQueryByHash(Coordinates center, double radius, String fieldName) {
		return new FilteredQuery( new MatchAllDocsQuery(),
				buildDistanceFilter(
						buildSpatialHashFilter( center, radius, fieldName ),
						center,
						radius,
						fieldName
				)
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
			longQuery = new BooleanQuery();
			( (BooleanQuery) longQuery).add( NumericRangeQuery.newDoubleRange( longitudeFieldName, boundingBox.getLowerLeft().getLongitude(),
					180.0, true, true ), BooleanClause.Occur.SHOULD );
			( (BooleanQuery) longQuery).add( NumericRangeQuery.newDoubleRange( longitudeFieldName, -180.0,
					boundingBox.getUpperRight().getLongitude(), true, true ), BooleanClause.Occur.SHOULD );
		}

		BooleanQuery boxQuery = new BooleanQuery();
		boxQuery.add( latQuery, BooleanClause.Occur.FILTER );
		boxQuery.add( longQuery, BooleanClause.Occur.FILTER );

		return new FilteredQuery(
				new MatchAllDocsQuery(),
				buildDistanceFilter(
						new QueryWrapperFilter( boxQuery ),
						center,
						radius,
						latitudeFieldName,
						longitudeFieldName
				)
		);
	}
}
