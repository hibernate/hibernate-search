/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Store;

/**
 * @author Gustavo Fernandes
 */
@Entity
@Indexed (index = "numeric_field_test")
public class PinPoint {

	@Id
	@Field(name = "numeric_id")
	@NumericField(forField = "numeric_id")
	private int id;

	@Field(store = Store.YES)
	private Integer stars;

	@ManyToOne
	@ContainedIn
	private Location location;

	public PinPoint(int id, int stars, Location location) {
		this.id = id;
		this.stars = stars;
		this.location = location;
	}

	public PinPoint() {
	}

	public void setLocation(Location location) {
		this.location = location;
	}
}
