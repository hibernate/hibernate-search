/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes;

import jakarta.persistence.Entity;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity(name = Hierarchy5_A_B_C.NAME)
@Indexed(index = Hierarchy5_A_B_C.NAME)
public class Hierarchy5_A_B_C extends Hierarchy5_A_B__MappedSuperClass {

	public static final String NAME = "H5_A_B_C";

	protected Hierarchy5_A_B_C() {
		// For Hibernate ORM
	}

	public Hierarchy5_A_B_C(int id) {
		super( id );
	}
}
