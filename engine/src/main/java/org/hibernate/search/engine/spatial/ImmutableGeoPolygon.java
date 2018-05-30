/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.spatial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.search.util.impl.common.CollectionHelper;
import org.hibernate.search.util.impl.common.Contracts;

public class ImmutableGeoPolygon implements GeoPolygon {

	private List<GeoPoint> points;

	public ImmutableGeoPolygon(List<GeoPoint> points) {
		Contracts.assertNotNull( points, "points" );

		this.points = CollectionHelper.toImmutableList( new ArrayList<>( points ) );
	}

	public ImmutableGeoPolygon(GeoPoint firstPoint, GeoPoint secondPoint, GeoPoint thirdPoint, GeoPoint fourthPoint, GeoPoint... additionalPoints) {
		Contracts.assertNotNull( firstPoint, "firstPoint" );
		Contracts.assertNotNull( secondPoint, "secondPoint" );
		Contracts.assertNotNull( thirdPoint, "thirdPoint" );
		Contracts.assertNotNull( fourthPoint, "fourthPoint" );

		List<GeoPoint> points = new ArrayList<>();
		points.add( firstPoint );
		points.add( secondPoint );
		points.add( thirdPoint );
		points.add( fourthPoint );
		Collections.addAll( points, additionalPoints );

		this.points = CollectionHelper.toImmutableList( points );
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

		if ( !that.points.equals( points ) ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return points.hashCode();
	}

	@Override
	public String toString() {
		return "GeoPolygon [points=" + points + "]";
	}
}
