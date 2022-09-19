/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.search.projection;

import jakarta.persistence.Basic;
import jakarta.persistence.Embeddable;

import org.hibernate.search.engine.spatial.GeoPoint;

@Embeddable
public class EmbeddableGeoPoint implements GeoPoint {

	public static EmbeddableGeoPoint of(double latitude, double longitude) {
		return new EmbeddableGeoPoint( latitude, longitude );
	}

	@Basic
	private Double latitude;
	@Basic
	private Double longitude;

	protected EmbeddableGeoPoint() {
		// For Hibernate ORM
	}

	private EmbeddableGeoPoint(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	@Override
	@Basic
	public double latitude() {
		return latitude;
	}

	@Override
	@Basic
	public double longitude() {
		return longitude;
	}
}
