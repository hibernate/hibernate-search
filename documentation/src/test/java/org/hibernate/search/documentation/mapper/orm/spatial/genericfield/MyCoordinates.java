/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.spatial.genericfield;

import javax.persistence.Basic;
import javax.persistence.Embeddable;

import org.hibernate.search.engine.spatial.GeoPoint;

//tag::include[]
@Embeddable
public class MyCoordinates implements GeoPoint { // <1>

	@Basic
	private Double latitude;

	@Basic
	private Double longitude;

	protected MyCoordinates() {
		// For Hibernate ORM
	}

	public MyCoordinates(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	@Override
	public double latitude() { // <2>
		return latitude;
	}

	@Override
	public double longitude() {
		return longitude;
	}
}
//end::include[]
