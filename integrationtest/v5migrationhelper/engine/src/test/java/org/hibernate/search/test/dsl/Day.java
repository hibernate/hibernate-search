/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.dsl;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;

/**
 * @author Gunnar Morling
 */
@Indexed
class Day {

	@DocumentId
	@Field(name = "idNumeric")
	@NumericField
	private int id;

	@Field
	private int number;

	public Day(int id, int number) {
		this.id = id;
		this.number = number;
	}

	public long getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}
}
