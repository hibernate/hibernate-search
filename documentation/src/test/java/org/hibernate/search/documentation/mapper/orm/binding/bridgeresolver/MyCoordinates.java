/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.bridgeresolver;

import jakarta.persistence.Basic;
import jakarta.persistence.Embeddable;

// Let's assume this type cannot be changed to implement GeoPoint.
@Embeddable
public class MyCoordinates {

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

	public double latitude() { // <2>
		return latitude;
	}

	public double longitude() {
		return longitude;
	}
}
