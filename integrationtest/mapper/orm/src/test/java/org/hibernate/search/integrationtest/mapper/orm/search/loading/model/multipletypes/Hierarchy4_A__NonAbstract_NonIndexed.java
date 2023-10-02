/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity(name = Hierarchy4_A__NonAbstract_NonIndexed.NAME)
public class Hierarchy4_A__NonAbstract_NonIndexed {

	public static final String NAME = "H4_A";

	@Id
	private Integer id;

	protected Hierarchy4_A__NonAbstract_NonIndexed() {
		// For Hibernate ORM
	}

	public Hierarchy4_A__NonAbstract_NonIndexed(int id) {
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
