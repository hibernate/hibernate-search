/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.test.embedded.path.prefixed;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.search.annotations.Field;

/**
 * @author Davide D'Alto
 */
@Entity
class EntityC {

	@Id
	@GeneratedValue
	public int id;

	@OneToOne(mappedBy = "indexed")
	public EntityB b;

	@OneToOne(mappedBy = "skipped")
	public EntityB b2;

	@Field
	public String field;

	@Field
	public String skipped = "skipped";

	public EntityC() {
	}

	public EntityC(String indexed) {
		this.field = indexed;
	}

}
