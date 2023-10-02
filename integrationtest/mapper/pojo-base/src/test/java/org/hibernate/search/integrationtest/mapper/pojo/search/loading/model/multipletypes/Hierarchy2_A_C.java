/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.multipletypes;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;

@SearchEntity(name = Hierarchy2_A_C.NAME)
@Indexed
public class Hierarchy2_A_C extends Hierarchy2_A__NonAbstract_Indexed {

	public static final String NAME = "H2_A_C";

	public Hierarchy2_A_C(int id) {
		super( id );
	}
}
