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

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.spatial.Coordinates;

/**
 * Spatial fields, ids generator and geometric calculation methods for use in SpatialFieldBridge
 *
 * @author Nicolas Helleringer <nicolas.helleringer@novacodex.net>
 * @author Mathieu Perez <mathieu.perez@novacodex.net>
 * @see org.hibernate.search.spatial.SpatialFieldBridgeByQuadTree
 * @see org.hibernate.search.spatial.SpatialFieldBridgeByRange
 */
public abstract class SpatialHelper {

	private static final double LOG2 = Math.log( 2 );

	/**
	 * Private constructor locking down utility class
	 */
	private SpatialHelper() { };

	/**
	 * Generate a Cell Index on one axis
	 *
	 * @param coordinate position to compute the Index for
	 * @param range range of the axis (-pi,pi)/(-90,90) => 2*pi/180 e.g
	 * @param quadTreeLevel Hox many time the range has been split in two
	 * @return the cell index on the axis
	 */
	public static int getCellIndex(double coordinate, double range, int quadTreeLevel) {
		return (int) Math.floor( Math.pow( 2, quadTreeLevel ) * coordinate / range );
	}

	/**
	 * Generate a Quad Tree Cell Id (with both Cell Index on both dimension in it) for a position
	 *
	 * @param point position to compute the Quad Tree Cell Id for
	 * @param quadTreeLevel Hox many time the dimensions have been split in two
	 * @return the cell id for the point at the given quad tree level
	 */
	public static String getQuadTreeCellId(Point point, int quadTreeLevel) {
		double[] indexablesCoordinates = projectToIndexSpace( point );
		int longitudeCellIndex = getCellIndex(
				indexablesCoordinates[0],
				GeometricConstants.PROJECTED_LONGITUDE_RANGE,
				quadTreeLevel
		);
		int latitudeCellIndex = getCellIndex(
				indexablesCoordinates[1],
				GeometricConstants.PROJECTED_LATITUDE_RANGE,
				quadTreeLevel
		);
		return formatQuadTreeCellId( longitudeCellIndex, latitudeCellIndex );
	}

	/**
	 * Generate a Quad Tree Cell Ids List covered by a bounding box
	 *
	 * @param lowerLeft lower left corner of the bounding box
	 * @param upperRight upper right corner of the bounding box
	 * @param quadTreeLevel quad tree level of the wanted cell ids
	 * @return List of ids of the cells containing the point
	 */
	public static List<String> getQuadTreeCellsIds(Point lowerLeft, Point upperRight, int quadTreeLevel) {
		double[] projectedLowerLeft = projectToIndexSpace( lowerLeft );
		int lowerLeftXIndex = getCellIndex(
				projectedLowerLeft[0],
				GeometricConstants.PROJECTED_LONGITUDE_RANGE,
				quadTreeLevel
		);
		int lowerLeftYIndex = getCellIndex(
				projectedLowerLeft[1],
				GeometricConstants.PROJECTED_LATITUDE_RANGE,
				quadTreeLevel
		);

		double[] projectedUpperRight = projectToIndexSpace( upperRight );
		int upperRightXIndex = getCellIndex(
				projectedUpperRight[0],
				GeometricConstants.PROJECTED_LONGITUDE_RANGE,
				quadTreeLevel
		);
		int upperRightYIndex = getCellIndex(
				projectedUpperRight[1],
				GeometricConstants.PROJECTED_LATITUDE_RANGE,
				quadTreeLevel
		);

		double[] projectedLowerRight = projectToIndexSpace( Point.fromDegrees( lowerLeft.getLatitude(), upperRight.getLongitude() ) );
		int lowerRightXIndex = getCellIndex(
				projectedLowerRight[0],
				GeometricConstants.PROJECTED_LONGITUDE_RANGE,
				quadTreeLevel
		);
		int lowerRightYIndex = getCellIndex(
				projectedLowerRight[1],
				GeometricConstants.PROJECTED_LATITUDE_RANGE,
				quadTreeLevel
		);

		double[] projectedUpperLeft = projectToIndexSpace( Point.fromDegrees( upperRight.getLatitude(), lowerLeft.getLongitude() ) );
		int upperLeftXIndex = getCellIndex(
				projectedUpperLeft[0],
				GeometricConstants.PROJECTED_LONGITUDE_RANGE,
				quadTreeLevel
		);
		int upperLeftYIndex = getCellIndex(
				projectedUpperLeft[1],
				GeometricConstants.PROJECTED_LATITUDE_RANGE,
				quadTreeLevel
		);

		final int startX = Math.min( Math.min( Math.min( lowerLeftXIndex, upperLeftXIndex ), upperRightXIndex ), lowerRightXIndex );
		final int endX = Math.max( Math.max( Math.max( lowerLeftXIndex, upperLeftXIndex ), upperRightXIndex ), lowerRightXIndex );

		final int startY = Math.min( Math.min( Math.min( lowerLeftYIndex, upperLeftYIndex ), upperRightYIndex ), lowerRightYIndex );
		final int endY = Math.max( Math.max( Math.max( lowerLeftYIndex, upperLeftYIndex ), upperRightYIndex ), lowerRightYIndex );

		List<String> quadTreeCellsIds = new ArrayList<String>( ( endX + 1 - startX ) * ( endY + 1 - startY ) );
		for ( int xIndex = startX; xIndex <= endX; xIndex++ ) {
			for ( int yIndex = startY; yIndex <= endY; yIndex++ ) {
				quadTreeCellsIds.add( formatQuadTreeCellId( xIndex, yIndex ) );
			}
		}

		return quadTreeCellsIds;
	}

	/**
	 * Generate a Quad Tree Cell Ids List for the bounding box of a circular search area
	 *
	 * @param center center of the search area
	 * @param radius radius of the search area
	 * @param quadTreeLevel Quad Tree level of the wanted cell ids
	 * @return List of the ids of the cells covering the bounding box of the given search discus
	 */
	public static List<String> getQuadTreeCellsIds(Coordinates center, double radius, int quadTreeLevel) {

		Rectangle boundingBox = Rectangle.fromBoundingCircle( center, radius );

		double lowerLeftLatitude = boundingBox.getLowerLeft().getLatitude();
		double lowerLeftLongitude = boundingBox.getLowerLeft().getLongitude();
		double upperRightLatitude = boundingBox.getUpperRight().getLatitude();
		double upperRightLongitude = boundingBox.getUpperRight().getLongitude();

		if ( upperRightLongitude < lowerLeftLongitude ) { // Box cross the 180 meridian
			final List<String> quadTreeCellsIds;
			quadTreeCellsIds = getQuadTreeCellsIds(
					Point.fromDegreesInclusive( lowerLeftLatitude, lowerLeftLongitude ),
					Point.fromDegreesInclusive( upperRightLatitude, GeometricConstants.LONGITUDE_DEGREE_RANGE / 2 ),
					quadTreeLevel
			);
			quadTreeCellsIds.addAll(
					getQuadTreeCellsIds(
							Point.fromDegreesInclusive(
									lowerLeftLatitude,
									GeometricConstants.LONGITUDE_DEGREE_RANGE / 2
								), Point.fromDegreesInclusive( upperRightLatitude, upperRightLongitude ), quadTreeLevel
					)
			);
			return quadTreeCellsIds;
		}
		else {
			return getQuadTreeCellsIds(
					Point.fromDegreesInclusive( lowerLeftLatitude, lowerLeftLongitude ),
					Point.fromDegreesInclusive( upperRightLatitude, upperRightLongitude ),
					quadTreeLevel
				);
		}
	}

	/**
	 * If point are searched at d distance from a point, a certain quad tree cell level will problem quad tree cell
	 * that are big enough to contain the search area but the smallest possible. By returning this level we ensure
	 * 4 Quad Tree Cell maximum will be needed to cover the search area (2 max on each axis because of search area
	 * crossing fixed bonds of the quad tree cells)
	 *
	 * @param searchRange
	 *            search range to be covered by the quad tree cells
	 * @return Return the best Quad Tree level for a given search radius.
	 */
	public static int findBestQuadTreeLevelForSearchRange(double searchRange) {

		double iterations = GeometricConstants.EARTH_EQUATOR_CIRCUMFERENCE_KM / ( 2.0d * searchRange );

		return (int) Math.max( 0, Math.ceil( Math.log( iterations ) / LOG2 ) );
	}

	/**
	 * Project a degree latitude/longitude point into a sinusoidal projection planar space for quad tree cell ids
	 * computation
	 *
	 * @param point
	 *            point to be projected
	 * @return array of projected coordinates
	 */
	public static double[] projectToIndexSpace(Point point) {
		double[] projectedCoordinates = new double[2];

		projectedCoordinates[0] = point.getLongitudeRad() * Math.cos( point.getLatitudeRad() );
		projectedCoordinates[1] = point.getLatitudeRad();

		return projectedCoordinates;
	}

	public static String formatFieldName(final int quadTreeLevel, final String fieldName) {
		return fieldName + "_HSSI_" + quadTreeLevel;
	}

	public static String formatLatitude(final String fieldName) {
		return fieldName + "_HSSI_Latitude";
	}

	public static String formatLongitude(final String fieldName) {
		return fieldName + "_HSSI_Longitude";
	}

	public static String formatQuadTreeCellId(final int xIndex, final int yIndex) {
		return xIndex + "|" + yIndex;
	}
}
