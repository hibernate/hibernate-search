package org.hibernate.search.test.engine;

import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Store;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;


/**
 * author: Gustavo Fernandes
 */
@Entity
@Indexed (index = "numeric_field_test")
public class PinPoint {

	@DocumentId @NumericField @Id
	private int id;

	@Field( store = Store.YES ) @NumericField
	private Integer stars;

	@ManyToOne @ContainedIn
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
