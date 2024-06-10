/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity(name = Hierarchy6_A__Abstract.NAME)
public abstract class Hierarchy6_A__Abstract {

	public static final String NAME = "H6_A";

	@Id
	private Integer id;

	protected Hierarchy6_A__Abstract() {
		// For Hibernate ORM
	}

	public Hierarchy6_A__Abstract(int id) {
		this.id = id;
	}

	public Integer getId() {
		return id;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + id + "]";
	}
}
