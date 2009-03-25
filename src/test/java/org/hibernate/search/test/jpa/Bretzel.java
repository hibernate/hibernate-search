//$Id$
package org.hibernate.search.test.jpa;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
public class Bretzel {
	@Id
	@GeneratedValue
	@DocumentId
	private Integer id;

	@Field(index = Index.UN_TOKENIZED)
	private float saltQty;

	@Field(index = Index.UN_TOKENIZED)
	private float weight;


	public Bretzel() {
	}

	public Bretzel(float saltQty, float weight) {
		this.saltQty = saltQty;
		this.weight = weight;
	}


	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public float getSaltQty() {
		return saltQty;
	}

	public void setSaltQty(float saltQty) {
		this.saltQty = saltQty;
	}

	public float getWeight() {
		return weight;
	}

	public void setWeight(float weight) {
		this.weight = weight;
	}
}
