/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes;

import jakarta.persistence.Entity;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity(name = Hierarchy7_A_D.NAME)
@Indexed(index = Hierarchy7_A_D.NAME)
// Does NOT implement Interface1, on contrary to B and C
public class Hierarchy7_A_D extends Hierarchy7_A__Abstract {

	public static final String NAME = "H7_A_D";

	protected Hierarchy7_A_D() {
		// For Hibernate ORM
	}

	public Hierarchy7_A_D(int id) {
		super( id );
	}
}
