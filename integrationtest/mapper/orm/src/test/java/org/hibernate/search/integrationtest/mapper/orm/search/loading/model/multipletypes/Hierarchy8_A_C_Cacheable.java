/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity(name = Hierarchy8_A_C_Cacheable.NAME)
@Indexed(index = Hierarchy8_A_C_Cacheable.NAME)
@Cacheable
public class Hierarchy8_A_C_Cacheable extends Hierarchy8_A__Abstract implements Interface2 {

	public static final String NAME = "H8_A_C";

	protected Hierarchy8_A_C_Cacheable() {
		// For Hibernate ORM
	}

	public Hierarchy8_A_C_Cacheable(int id) {
		super( id );
	}
}
