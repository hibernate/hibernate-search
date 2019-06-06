/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.spatial;

/**
 * A bounding box in the geocentric coordinate system.
 */
public interface GeoBoundingBox {

	/**
	 * @return The top-left coordinates of this bounding-box.
	 */
	GeoPoint getTopLeft();

	/**
	 * @return The bottom-right coordinates of this bounding-box.
	 */
	GeoPoint getBottomRight();

	/**
	 * Create a {@link GeoBoundingBox} from the top-left and bottom-right corners.
	 *
	 * @param topLeft The coordinates of the top-left corner of the bounding box.
	 * @param bottomRight The coordinates of the bottom-right corner of the bounding box.
	 * @return The corresponding {@link GeoBoundingBox}.
	 */
	static GeoBoundingBox of(GeoPoint topLeft, GeoPoint bottomRight) {
		return new ImmutableGeoBoundingBox( topLeft, bottomRight );
	}

	/**
	 * Create a {@link GeoBoundingBox} from the latitude and longitude of the top-left and bottom-right corners.
	 *
	 * @param topLeftLatitude The latitude of the top-left corner of the bounding box.
	 * @param topLeftLongitude The longitude of the top-left corner of the bounding box.
	 * @param bottomRightLatitude The latitude of the bottom-right corner of the bounding box.
	 * @param bottomRightLongitude The longitude of the bottom-right corner of the bounding box.
	 * @return The corresponding {@link GeoBoundingBox}.
	 */
	static GeoBoundingBox of(double topLeftLatitude, double topLeftLongitude,
			double bottomRightLatitude, double bottomRightLongitude) {
		return new ImmutableGeoBoundingBox(
				GeoPoint.of( topLeftLatitude, topLeftLongitude ),
				GeoPoint.of( bottomRightLatitude, bottomRightLongitude )
		);
	}
}
