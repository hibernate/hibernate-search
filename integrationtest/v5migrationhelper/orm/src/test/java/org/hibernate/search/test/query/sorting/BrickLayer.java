/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.query.sorting;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.SortableField;

/**
 * @author Gunnar Morling
 */
@Entity
@Indexed(index = "contractor")
public class BrickLayer {

	@Id
	private int id;

	@SortableField(forField = "sortName")
	@Fields({
			@Field,
			@Field(name = "sortName", analyze = Analyze.NO)
	})
	private String name;

	@SortableField(forField = "sortLastName")
	@Fields({
			@Field,
			@Field(name = "sortLastName", analyze = Analyze.NO)
	})
	private String lastName;

	BrickLayer() {
	}

	public BrickLayer(int id, String name, String lastName) {
		this.id = id;
		this.name = name;
		this.lastName = lastName;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
}
