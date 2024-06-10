/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.multipletypes;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;

@SearchEntity(name = Hierarchy1_A_B.NAME)
@Indexed
public class Hierarchy1_A_B extends Hierarchy1_A__Abstract {

	public static final String NAME = "H1_A_B";

	public Hierarchy1_A_B(int id) {
		super( id );
	}
}
