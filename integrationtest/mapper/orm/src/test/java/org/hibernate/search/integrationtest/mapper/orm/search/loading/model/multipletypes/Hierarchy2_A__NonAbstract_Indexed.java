/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity(name = Hierarchy2_A__NonAbstract_Indexed.NAME)
@Indexed(index = Hierarchy2_A__NonAbstract_Indexed.NAME)
public class Hierarchy2_A__NonAbstract_Indexed {

	public static final String NAME = "H2_A";

	@Id
	private Integer id;

	protected Hierarchy2_A__NonAbstract_Indexed() {
		// For Hibernate ORM
	}

	public Hierarchy2_A__NonAbstract_Indexed(int id) {
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
