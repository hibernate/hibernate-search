/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.query;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
public class Husband {
	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	private Long id;

	@Field(store = Store.YES)
	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	private String lastName;

	@ManyToOne(fetch = FetchType.LAZY)
	public Spouse getSpouse() {
		return spouse;
	}

	public void setSpouse(Spouse spouse) {
		this.spouse = spouse;
	}

	private Spouse spouse;
}
