package org.hibernate.search.test.spatial;

import javax.persistence.Embeddable;

import org.hibernate.search.annotations.Spatial;
import org.hibernate.search.spatial.Coordinates;

/**
 * Created with IntelliJ IDEA.
 * User: nicolashelleringer
 * Date: 30/05/12
 * Time: 16:50
 * To change this template use File | Settings | File Templates.
 */
@Embeddable
public class Position {
	String address;
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

	public Position() {}
}
