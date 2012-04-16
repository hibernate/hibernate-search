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
package org.hibernate.search.spatial.impl;

import org.apache.lucene.search.*;
import org.hibernate.search.spatial.SpatialFieldBridge;

import java.util.List;

/**
 * The SpatialQueryBuilder holds builder methods for Grid, Distance and Spatial (Grid+Distance) filters and queries
 *
 * @author Nicolas Helleringer <nicolas.helleringer@novacodex.net>
 */
public abstract class SpatialQueryBuilderFromPoint {
	/**
	 * Returns a Lucene filter which rely on Hibernate Search Spatial
	 * grid indexation to filter document at radius
	 *
	 * @param center center of the search discus
	 * @param radius distance max to center in km
	 * @param fieldName name of the Lucene Field implementing Coordinates
	 *
	 * @return Lucene filter to be used in a Query
	 *
	 * @see org.apache.lucene.search.Query
	 * @see org.hibernate.search.spatial.Coordinates
	 * @see org.apache.lucene.search.Filter
	 */
	public static Filter buildGridFilter(Point center, double radius, String fieldName) {
		int bestGridLevel = GridHelper.findBestGridLevelForSearchRange( 2.0d * radius );
		if ( bestGridLevel > SpatialFieldBridge.DEFAULT_BOTTOM_GRID_LEVEL ) {
			bestGridLevel = SpatialFieldBridge.DEFAULT_BOTTOM_GRID_LEVEL;
		}
		List<String> gridCellsIds = GridHelper.getGridCellsIds( center, radius, bestGridLevel );
		return new GridFilter( gridCellsIds, GridHelper.formatFieldName( bestGridLevel, fieldName ) );
	}

	/**
	 * Returns a Lucene filter to fine filter document by distance
	 *
	 * @param center center of the search discus
	 * @param radius distance max to center in km
	 * @param coordinatesField name of the Lucene Field implementing Coordinates
	 *
	 * @return Lucene filter to be used in a Query
	 *
	 * @param previousFilter	preceding filter in filter chain
	 * Warning if passed null DistanceFilter constructor use a
	 * filter wrapped match all query (time/resource consuming !)
	 * @see org.apache.lucene.search.Query
	 * @see org.hibernate.search.spatial.Coordinates
	 * @see DistanceFilter
	 * @see Filter
	 */
	public static Filter buildDistanceFilter(Filter previousFilter, Point center, double radius, String coordinatesField) {
		return new DistanceFilter( previousFilter, center, radius, coordinatesField );
	}

	/**
	 * Returns a Lucene filter to fine filter document by distance
	 *
	 * @param center center of the search discus
	 * @param radius distance max to center in km
	 * @param latitudeField name of the Lucene Field hosting latitude
	 * @param longitudeField name of the Lucene Field hosting longitude
	 *
	 * @return Lucene filter to be used in a Query
	 *
	 * @param previousFilter	preceding filter in filter chain
	 * Warning if passed null DistanceFilter constructor use a
	 * filter wrapped match all query (time/ressource consuming !)
	 * @see org.apache.lucene.search.Query
	 * @see org.hibernate.search.spatial.Coordinates
	 * @see DistanceFilter
	 * @see Filter
	 */
	public static Filter buildDistanceFilter(Filter previousFilter, Point center, double radius, String latitudeField, String longitudeField) {
		return new DistanceFilter( previousFilter, center, radius, latitudeField, longitudeField );
	}

	/**
	 * Returns a Lucene Query which rely on Hibernate Search Spatial
	 * grid indexation to filter document at radius by wrapping a
	 * GridFilter into a ConstantScoreQuery
	 *
	 * @param center center of the search discus
	 * @param radius distance max to center in km
	 * @param fieldName name of the Lucene Field implementing Coordinates
	 *
	 * @return Lucene Query to be used in a search
	 *
	 * @see org.apache.lucene.search.Query
	 * @see org.hibernate.search.spatial.Coordinates
	 * @see org.apache.lucene.search.ConstantScoreQuery
	 */
	public static Query buildGridQuery(Point center, double radius, String fieldName) {
		return new ConstantScoreQuery( buildGridFilter( center, radius, fieldName ) );
	}


	/**
	 * Returns a Lucene Query searching directly by computing distance against
	 * all docs in the index (costly !)
	 *
	 * @param center center of the search discus
	 * @param radius distance max to center in km
	 * @param fieldName name of the Lucene Field implementing Coordinates
	 *
	 * @return Lucene Query to be used in a search
	 *
	 * @see Query
	 * @see org.hibernate.search.spatial.Coordinates
	 */
	public static Query buildDistanceQuery(Point center, double radius, String fieldName) {
		Filter allFilter = new QueryWrapperFilter( new MatchAllDocsQuery() );
		return new ConstantScoreQuery( buildDistanceFilter( allFilter, center, radius, fieldName ) );
	}

	/**
	 * Returns a Lucene Query which rely on Hibernate Search Spatial
	 * grid indexation to filter document at radius and filter its results
	 * by a fine DistanceFilter
	 *
	 * @param center center of the search discus
	 * @param radius distance max to center in km
	 * @param fieldName name of the Lucene Field implementing Coordinates
	 *
	 * @return Lucene Query to be used in a search
	 *
	 * @see Query
	 * @see org.hibernate.search.spatial.Coordinates
	 */
	public static Query buildSpatialQuery(Point center, double radius, String fieldName) {
		return new ConstantScoreQuery(
				buildDistanceFilter(
						buildGridFilter( center, radius, fieldName ),
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
	 * @param center center of the search discus
	 * @param radius distance max to center in km
	 *
	 * @return Lucene Query to be used in a search
	 *
	 * @see Query
	 * @see org.hibernate.search.spatial.Coordinates
	 */
	public static Query buildSimpleSpatialQuery(Point center, double radius) {

		Rectangle boundingBox = Rectangle.fromBoundingCircle( center, radius );

		Query latQuery= NumericRangeQuery.newDoubleRange( "latitude_hibernate_search_spatial", boundingBox.getLowerLeft().getLatitude(),
				boundingBox.getUpperRight().getLatitude(), true, true);

		Query longQuery= NumericRangeQuery.newDoubleRange( "longitude_hibernate_search_spatial", boundingBox.getLowerLeft().getLongitude(),
				boundingBox.getUpperRight().getLongitude(), true, true);

		BooleanQuery boxQuery = new BooleanQuery();
		boxQuery.add(latQuery, BooleanClause.Occur.MUST);
		boxQuery.add(longQuery, BooleanClause.Occur.MUST);

		return new ConstantScoreQuery(
				buildDistanceFilter(
						new QueryWrapperFilter( boxQuery ),
						center,
						radius,
						"latitude_hibernate_search_spatial",
						"longitude_hibernate_search_spatial"
				)
		);
	}
}
