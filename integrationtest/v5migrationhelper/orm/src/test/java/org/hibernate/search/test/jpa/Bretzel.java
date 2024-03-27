/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

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

	@Field(analyze = Analyze.NO)
	private int saltQty;

	@Field(analyze = Analyze.NO)
	private int weight;

	public Bretzel() {
	}

	public Bretzel(int saltQty, int weight) {
		this.saltQty = saltQty;
		this.weight = weight;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public int getSaltQty() {
		return saltQty;
	}

	public void setSaltQty(int saltQty) {
		this.saltQty = saltQty;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}
}
