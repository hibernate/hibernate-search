/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.spatial;

import java.util.Objects;

import org.hibernate.search.util.impl.common.Contracts;

public class ImmutableGeoBoundingBox implements GeoBoundingBox {

	private GeoPoint topLeft;

	private GeoPoint bottomRight;

	public ImmutableGeoBoundingBox(GeoPoint topLeft, GeoPoint bottomRight) {
		Contracts.assertNotNull( topLeft, "topLeft" );
		Contracts.assertNotNull( bottomRight, "bottomRight" );

		this.topLeft = topLeft;
		this.bottomRight = bottomRight;
	}

	public ImmutableGeoBoundingBox(double topLeftLatitude, double topLeftLongitude, double bottomRightLatitude, double bottomRightLongitude) {
		this.topLeft = new ImmutableGeoPoint( topLeftLatitude, topLeftLongitude );
		this.bottomRight = new ImmutableGeoPoint( bottomRightLatitude, bottomRightLongitude );
	}

	@Override
	public GeoPoint getTopLeft() {
		return topLeft;
	}

	@Override
	public GeoPoint getBottomRight() {
		return bottomRight;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}

		if ( obj == null || getClass() != obj.getClass() ) {
			return false;
		}

		ImmutableGeoBoundingBox that = (ImmutableGeoBoundingBox) obj;

		return Objects.equals( that.topLeft, topLeft ) &&
				Objects.equals( that.bottomRight, bottomRight );
	}

	@Override
	public int hashCode() {
		return Objects.hash( topLeft, bottomRight );
	}

	@Override
	public String toString() {
		return "GeoBoundingBox [topLeft=" + topLeft + ", bottomRight=" + bottomRight + "]";
	}
}
