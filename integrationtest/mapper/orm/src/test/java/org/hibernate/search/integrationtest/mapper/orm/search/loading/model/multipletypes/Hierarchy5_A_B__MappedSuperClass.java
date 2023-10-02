/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes;

import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class Hierarchy5_A_B__MappedSuperClass extends Hierarchy5_A__Abstract {

	public static final String NAME = "H5_A_B";

	protected Hierarchy5_A_B__MappedSuperClass() {
		// For Hibernate ORM
	}

	public Hierarchy5_A_B__MappedSuperClass(int id) {
		super( id );
	}
}
