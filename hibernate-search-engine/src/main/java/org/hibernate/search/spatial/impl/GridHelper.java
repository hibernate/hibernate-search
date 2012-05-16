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

/**
 * Grid fields,Ids generator and geometric calculation methods for use in SpatialFieldBridge
 *
 * @author Nicolas Helleringer <nicolas.helleringer@novacodex.net>
 * @author Mathieu Perez <mathieu.perez@novacodex.net>
 * @see org.hibernate.search.spatial.SpatialFieldBridgeByGrid
 */
public abstract class GridHelper {

	private static final double LOG2 = Math.log( 2 );

	/**
	 * Private construtor locking down utility class
	 */
	private GridHelper() {};

	/**
	 * Generate a Cell Index on one axis
	 *
	 * @param coordinate position to compute the Index for
	 * @param range range of the axis (-pi,pi)/(-90,90) => 2*pi/180 e.g
	 * @param gridLevel Hox many time the range has been split in two
	 *                  
	 * @return the cell index on the axis
	 */
	public static int getCellIndex(double coordinate, double range, int gridLevel) {
		return ( int ) Math.floor( Math.pow( 2, gridLevel ) * coordinate / range );
	}

	/**
	 * Generate a Grid Cell Id (with both Cell Index on both dimension in it) for a position
	 *
	 * @param point position to compute the Grid Cell Id for
	 * @param gridLevel Hox many time the dimensions have been split in two
	 *                  
	 * @return the cell id for the point at the given grid level
	 */
	public static String getGridCellId(Point point, int gridLevel) {
		double[] indexablesCoordinates = projectToIndexSpace( point );
		int longitudeCellIndex = getCellIndex(
				indexablesCoordinates[0],
				GeometricConstants.PROJECTED_LONGITUDE_RANGE,
				gridLevel
		);
		int latitudeCellIndex = getCellIndex(
				indexablesCoordinates[1],
				GeometricConstants.PROJECTED_LATITUDE_RANGE,
				gridLevel
		);
		return formatGridCellId( longitudeCellIndex, latitudeCellIndex );
	}

	/**
	 * Generate a Grid Cell Ids List covered by a bounding box
	 *
	 * @param lowerLeft lower left corner of the bounding box
	 * @param upperRight upper right corner of the bounding box
	 * @param gridLevel grid level of the wanted cell ids
	 *                  
	 * @return List of ids of the cells containing the point
	 */
	public static List<String> getGridCellsIds(Point lowerLeft, Point upperRight, int gridLevel) {
		double[] projectedLowerLeft = projectToIndexSpace( lowerLeft );
		int lowerLeftXIndex = getCellIndex(
				projectedLowerLeft[0],
				GeometricConstants.PROJECTED_LONGITUDE_RANGE,
				gridLevel
		);
		int lowerLeftYIndex = getCellIndex(
				projectedLowerLeft[1],
				GeometricConstants.PROJECTED_LATITUDE_RANGE,
				gridLevel
		);

		double[] projectedUpperRight = projectToIndexSpace( upperRight );
		int upperRightXIndex = getCellIndex(
				projectedUpperRight[0],
				GeometricConstants.PROJECTED_LONGITUDE_RANGE,
				gridLevel
		);
		int upperRightYIndex = getCellIndex(
				projectedUpperRight[1],
				GeometricConstants.PROJECTED_LATITUDE_RANGE,
				gridLevel
		);

		double[] projectedLowerRight= projectToIndexSpace( Point.fromDegrees( lowerLeft.getLatitude(), upperRight.getLongitude() ) );
		int lowerRightXIndex = getCellIndex(
				projectedLowerRight[0],
				GeometricConstants.PROJECTED_LONGITUDE_RANGE,
				gridLevel
		);
		int lowerRightYIndex = getCellIndex(
				projectedLowerRight[1],
				GeometricConstants.PROJECTED_LATITUDE_RANGE,
				gridLevel
		);

		double[] projectedUpperLeft = projectToIndexSpace( Point.fromDegrees( upperRight.getLatitude(), lowerLeft.getLongitude() ) );
		int upperLeftXIndex = getCellIndex(
				projectedUpperLeft[0],
				GeometricConstants.PROJECTED_LONGITUDE_RANGE,
				gridLevel
		);
		int upperLeftYIndex = getCellIndex(
				projectedUpperLeft[1],
				GeometricConstants.PROJECTED_LATITUDE_RANGE,
				gridLevel
		);


		int startX, endX;
		startX= Math.min(Math.min(Math.min(lowerLeftXIndex,upperLeftXIndex),upperRightXIndex),lowerRightXIndex);
		endX= Math.max(Math.max(Math.max(lowerLeftXIndex,upperLeftXIndex),upperRightXIndex),lowerRightXIndex);

		int startY, endY;
		startY= Math.min(Math.min(Math.min(lowerLeftYIndex,upperLeftYIndex),upperRightYIndex),lowerRightYIndex);
		endY= Math.max(Math.max(Math.max(lowerLeftYIndex,upperLeftYIndex),upperRightYIndex),lowerRightYIndex);

		List<String> gridCellsIds = new ArrayList<String>((endX+1-startX)*(endY+1-startY));
		int xIndex, yIndex;
		for ( xIndex = startX; xIndex <= endX; xIndex++ ) {
			for ( yIndex = startY; yIndex <= endY; yIndex++ ) {
				gridCellsIds.add( formatGridCellId( xIndex, yIndex ) );
			}
		}

		return gridCellsIds;
	}

	/**
	 * Generate a Grid Cell Ids List for the bounding box of a circular search area
	 *
	 * @param center center of the search area
	 * @param radius radius of the search area
	 * @param gridLevel grid level of the wanted cell ids
	 *                  
	 * @return List of the ids of the cells covering the bounding box of the given search discus
	 */
	public static List<String> getGridCellsIds(Point center, double radius, int gridLevel) {

		Rectangle boundingBox = Rectangle.fromBoundingCircle( center, radius );

		double lowerLeftLatitude = boundingBox.getLowerLeft().getLatitude();
		double lowerLeftLongitude = boundingBox.getLowerLeft().getLongitude();
		double upperRightLatitude = boundingBox.getUpperRight().getLatitude();
		double upperRightLongitude = boundingBox.getUpperRight().getLongitude();

		if ( upperRightLongitude < lowerLeftLongitude ) { // Box cross the 180 meridian
			List<String> gridCellsIds;
			gridCellsIds = getGridCellsIds(
					Point.fromDegreesInclusive( lowerLeftLatitude, lowerLeftLongitude ),
					Point.fromDegreesInclusive( upperRightLatitude, GeometricConstants.LONGITUDE_DEGREE_RANGE / 2 ),
					gridLevel
			);
			gridCellsIds.addAll(
					getGridCellsIds(
							Point.fromDegreesInclusive(
									lowerLeftLatitude,
									-GeometricConstants.LONGITUDE_DEGREE_RANGE / 2
							), Point.fromDegreesInclusive( upperRightLatitude, upperRightLongitude ), gridLevel
					)
			);
			return gridCellsIds;
		}
		else {
			return getGridCellsIds(
					Point.fromDegreesInclusive( lowerLeftLatitude, lowerLeftLongitude ),
					Point.fromDegreesInclusive( upperRightLatitude, upperRightLongitude ),
					gridLevel
			);
		}
	}

	/**
	 * If point are searched at d distance from a point, a certain grid cell level will problem grid cell that are
	 * big enough to contain the search area but the smallest possible. By returning this level we ensure 4 Grid Cell
	 * maximum will be needed to cover the search area (2 max on each axis because of search area crossing fixed bonds
	 * of the grid cells)
	 *
	 * @param searchRange search range to be covered by the grid cells
	 *                    
	 * @return Return the best Grid level for a given search radius.
	 */
	public static int findBestGridLevelForSearchRange(double searchRange) {

		double iterations = GeometricConstants.EARTH_EQUATOR_CIRCUMFERENCE_KM / ( 2.0d * searchRange );

		return ( int ) Math.max(  0, Math.ceil( Math.log( iterations ) / LOG2 ));
	}

	/**
	 * Project a degree latitude/longitude point into a sinusoidal projection planar space for grid cell ids computation
	 *
	 * @param point point to be projected
	 *
	 * @return array of projected coordinates
	 */
	public static double[] projectToIndexSpace(Point point) {
		double[] projectedCoordinates = new double[2];

		projectedCoordinates[0] = point.getLongitudeRad() * Math.cos( point.getLatitudeRad() );
		projectedCoordinates[1] = point.getLatitudeRad();

		return projectedCoordinates;
	}

	public static String formatFieldName(int gridLevel, String fieldName) {
		return fieldName+"_HSSI_"+gridLevel;
	}

	public static String formatLatitude(String fieldName) {
		return fieldName+"_HSSI_Latitude";
	}

	public static String formatLongitude(String fieldName) {
		return fieldName+"_HSSI_Longitude";
	}

	public static String formatGridCellId(int xIndex, int yIndex) {
		return xIndex+"|"+yIndex;
	}
}