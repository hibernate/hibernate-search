/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine.numeric;

import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Store;

/**
 * @author Gustavo Fernandes
 */
@Indexed (index = "numeric_field_test")
class PinPoint {

	@DocumentId
	@Field(name = "numeric_id")
	@NumericField(forField = "numeric_id")
	private int id;

	@Field(store = Store.YES)
	private Integer stars;

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
