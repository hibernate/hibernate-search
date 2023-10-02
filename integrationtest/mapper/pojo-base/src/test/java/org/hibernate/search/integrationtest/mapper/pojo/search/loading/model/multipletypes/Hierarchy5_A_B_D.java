/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.multipletypes;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;

@SearchEntity(name = Hierarchy5_A_B_D.NAME)
@Indexed
public class Hierarchy5_A_B_D extends Hierarchy5_A_B__MappedSuperClass {

	public static final String NAME = "H5_A_B_D";

	public Hierarchy5_A_B_D(int id) {
		super( id );
	}
}
