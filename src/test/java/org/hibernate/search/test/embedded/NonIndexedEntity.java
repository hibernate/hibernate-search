package org.hibernate.search.test.embedded;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class NonIndexedEntity {
	@Id
	@GeneratedValue
	private int id;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
