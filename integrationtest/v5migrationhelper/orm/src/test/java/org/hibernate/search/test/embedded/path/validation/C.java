/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.test.embedded.path.validation;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.search.annotations.Field;

/**
 * @author Davide D'Alto
 */
@Entity
public class C {

	@Id
	@GeneratedValue
	public int id;

	@OneToOne(mappedBy = "c")
	public B b;

	@OneToOne(mappedBy = "skipped")
	public B b2;

	@Field
	public String indexed;

	@Field
	public String notIndexed = "notIndexed";

	public C() {
	}

	public C(String indexed) {
		this.indexed = indexed;
	}

}
