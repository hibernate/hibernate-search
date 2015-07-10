/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.metadata;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Spatial;
import org.hibernate.search.spatial.Coordinates;

/**
 * @author Davide D'Alto
 */
@Indexed
public class SnafuWithCoordinates {

	@DocumentId
	private long id;

	double latitude;

	double longitude;

	@Spatial
	public Coordinates getLocation() {
		return new Coordinates() {
			@Override
			public Double getLatitude() {
				return latitude;
			}

			@Override
			public Double getLongitude() {
				return longitude;
			}
		};
	}

}
