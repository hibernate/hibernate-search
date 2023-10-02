/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity(name = Hierarchy6_A_C_Cacheable.NAME)
@Indexed(index = Hierarchy6_A_C_Cacheable.NAME)
@Cacheable
public class Hierarchy6_A_C_Cacheable extends Hierarchy6_A__Abstract {

	public static final String NAME = "H6_A_C";

	protected Hierarchy6_A_C_Cacheable() {
		// For Hibernate ORM
	}

	public Hierarchy6_A_C_Cacheable(int id) {
		super( id );
	}
}
