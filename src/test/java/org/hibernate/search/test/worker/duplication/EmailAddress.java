// $Id$
package org.hibernate.search.test.worker.duplication;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Store;

/**
 * Test entity for HSEARCH-257.
 *
 * @author Hardy Ferentschik
 */
@Entity
public class EmailAddress implements Serializable {
	@Id
	@GeneratedValue
	@DocumentId
	private int id;

	private boolean isDefaultAddress;

	@Field(store = Store.YES, index = Index.NO)
	private String address;

	public EmailAddress() {
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public boolean isDefaultAddress() {
		return isDefaultAddress;
	}

	public void setDefaultAddress(boolean isDefault) {
		isDefaultAddress = isDefault;
	}
}
