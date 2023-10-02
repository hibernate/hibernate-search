/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes;

import jakarta.persistence.Entity;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity(name = Hierarchy4_A_C__integer2DocumentId.NAME)
@Indexed(index = Hierarchy4_A_C__integer2DocumentId.NAME)
public class Hierarchy4_A_C__integer2DocumentId extends Hierarchy4_A__NonAbstract_NonIndexed {

	public static final String NAME = "H4_A_C";

	@DocumentId
	private int integer2;

	protected Hierarchy4_A_C__integer2DocumentId() {
		// For Hibernate ORM
	}

	public Hierarchy4_A_C__integer2DocumentId(int id, int integer2) {
		super( id );
		this.integer2 = integer2;
	}
}
