/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.embedded.sorting;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.SortableField;

/**
 * @author Davide D'Alto
 */
@Entity
@Indexed
class Villain {

	private static final String ID_SORT = "id_sort";

	@Id
	@SortableField
	@GeneratedValue
	private Integer id;

	@OneToOne
	private Hero hero;

	@Field
	private String name;

	public Villain() {
	}

	public Villain(Integer id, String name) {
		super();
		this.id = id;
		this.name = name;
	}

	@Field(name = ID_SORT)
	@SortableField(forField = ID_SORT)
	@NumericField(forField = ID_SORT)
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Hero getHero() {
		return hero;
	}

	public void setHero(Hero hero) {
		this.hero = hero;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
