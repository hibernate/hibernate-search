/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes;

import jakarta.persistence.Entity;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity(name = Hierarchy1_A_B.NAME)
@Indexed(index = Hierarchy1_A_B.NAME)
public class Hierarchy1_A_B extends Hierarchy1_A__Abstract {

	public static final String NAME = "H1_A_B";

	protected Hierarchy1_A_B() {
		// For Hibernate ORM
	}

	public Hierarchy1_A_B(int id) {
		super( id );
	}
}
