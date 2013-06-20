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

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.FilteredQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;

import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.spatial.SpatialFieldBridgeByQuadTree;

import java.util.List;

/**
 * The SpatialQueryBuilder holds builder methods for Quad Tree, Distance and Spatial (Quad Tree+Distance) filters
 * and queries
 *
 * @author Nicolas Helleringer <nicolas.helleringer@novacodex.net>
 */
public abstract class SpatialQueryBuilderFromCoordinates {

	/**
	 * Returns a Lucene filter which rely on Hibernate Search Spatial
	 * quad tree indexation to filter document at radius
	 *
	 * @param center center of the search discus
	 * @param radius distance max to center in km
	 * @param fieldName name of the Lucene Field implementing Coordinates
	 * @return Lucene filter to be used in a Query
	 * @see org.apache.lucene.search.Query
	 * @see org.hibernate.search.spatial.Coordinates
	 * @see org.apache.lucene.search.Filter
	 */
	public static Filter buildQuadTreeFilter(Coordinates center, double radius, String fieldName) {
		int bestQuadTreeLevel = SpatialHelper.findBestQuadTreeLevelForSearchRange( 2.0d * radius );
		if ( bestQuadTreeLevel > SpatialFieldBridgeByQuadTree.DEFAULT_BOTTOM_QUAD_TREE_LEVEL ) {
			bestQuadTreeLevel = SpatialFieldBridgeByQuadTree.DEFAULT_BOTTOM_QUAD_TREE_LEVEL;
		}
		List<String> quadTreeCellsIds = SpatialHelper.getQuadTreeCellsIds( center, radius, bestQuadTreeLevel );
		return new QuadTreeFilter( quadTreeCellsIds, SpatialHelper.formatFieldName( bestQuadTreeLevel, fieldName ) );
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
	 * quad tree indexation to filter document at radius by wrapping a
	 * QuadTreeFilter
	 *
	 * @param center center of the search discus
	 * @param radius distance max to center in km
	 * @param fieldName name of the Lucene Field implementing Coordinates
	 * @return Lucene Query to be used in a search
	 * @see org.apache.lucene.search.Query
	 * @see org.hibernate.search.spatial.Coordinates
	 */
	public static Query buildQuadTreeQuery(Coordinates center, double radius, String fieldName) {
		return new FilteredQuery( new MatchAllDocsQuery(), buildQuadTreeFilter( center, radius, fieldName ) );
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
	 * quad tree indexation to filter documents at radius and filter its results
	 * by a fine DistanceFilter
	 *
	 * @param center center of the search discus
	 * @param radius distance max to center in km
	 * @param fieldName name of the Lucene Field implementing Coordinates
	 * @return Lucene Query to be used in a search
	 * @see Query
	 * @see org.hibernate.search.spatial.Coordinates
	 */
	public static Query buildSpatialQueryByQuadTree(Coordinates center, double radius, String fieldName) {
		return new FilteredQuery( new MatchAllDocsQuery(),
				buildDistanceFilter(
						buildQuadTreeFilter( center, radius, fieldName ),
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
		boxQuery.add( latQuery, BooleanClause.Occur.MUST );
		boxQuery.add( longQuery, BooleanClause.Occur.MUST );

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
