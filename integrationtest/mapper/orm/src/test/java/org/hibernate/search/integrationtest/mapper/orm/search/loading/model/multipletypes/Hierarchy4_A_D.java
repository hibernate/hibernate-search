/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes;

import jakarta.persistence.Entity;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity(name = Hierarchy4_A_D.NAME)
@Indexed(index = Hierarchy4_A_D.NAME)
public class Hierarchy4_A_D extends Hierarchy4_A__NonAbstract_NonIndexed {

	public static final String NAME = "H4_A_D";

	protected Hierarchy4_A_D() {
		// For Hibernate ORM
	}

	public Hierarchy4_A_D(int id) {
		super( id );
	}
}
