//$Id$
package org.hibernate.search.test;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
public class Clock {
	private Integer id;
	private String brand;

	public Clock(Integer id, String brand) {
		this.id = id;
		this.brand = brand;
	}

	@Field(index= Index.TOKENIZED, store= Store.YES) public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}

	@Id @DocumentId
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
}
