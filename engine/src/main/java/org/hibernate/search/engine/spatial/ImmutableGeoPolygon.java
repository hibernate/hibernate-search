/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.spatial;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.impl.common.CollectionHelper;
import org.hibernate.search.util.impl.common.Contracts;
import org.hibernate.search.util.impl.common.LoggerFactory;

final class ImmutableGeoPolygon implements GeoPolygon {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private List<GeoPoint> points;

	ImmutableGeoPolygon(List<GeoPoint> points) {
		Contracts.assertNotNull( points, "points" );

		GeoPoint firstPoint = points.get( 0 );
		GeoPoint lastPoint = points.get( points.size() - 1 );
		if ( !firstPoint.equals( lastPoint ) ) {
			throw log.invalidGeoPolygonFirstPointNotIdenticalToLastPoint( firstPoint, lastPoint );
		}

		this.points = CollectionHelper.toImmutableList( new ArrayList<>( points ) );
	}

	@Override
	public List<GeoPoint> getPoints() {
		return points;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}

		if ( obj == null || getClass() != obj.getClass() ) {
			return false;
		}

		ImmutableGeoPolygon that = (ImmutableGeoPolygon) obj;

		return that.points.equals( points );
	}

	@Override
	public int hashCode() {
		return points.hashCode();
	}

	@Override
	public String toString() {
		return "ImmutableGeoPolygon[points=" + points + "]";
	}
}
