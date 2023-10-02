/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes;

import jakarta.persistence.Entity;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity(name = Hierarchy4_A_B__integer1DocumentId.NAME)
@Indexed(index = Hierarchy4_A_B__integer1DocumentId.NAME)
public class Hierarchy4_A_B__integer1DocumentId extends Hierarchy4_A__NonAbstract_NonIndexed {

	public static final String NAME = "H4_A_B";

	@DocumentId
	private int integer1;

	protected Hierarchy4_A_B__integer1DocumentId() {
		// For Hibernate ORM
	}

	public Hierarchy4_A_B__integer1DocumentId(int id, int integer1) {
		super( id );
		this.integer1 = integer1;
	}
}
