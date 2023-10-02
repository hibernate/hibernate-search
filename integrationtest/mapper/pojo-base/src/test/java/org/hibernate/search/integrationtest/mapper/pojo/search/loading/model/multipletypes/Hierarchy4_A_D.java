/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.multipletypes;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;

@SearchEntity(name = Hierarchy4_A_D.NAME)
@Indexed
public class Hierarchy4_A_D extends Hierarchy4_A__NonAbstract_NonIndexed {

	public static final String NAME = "H4_A_D";

	public Hierarchy4_A_D(int id) {
		super( id );
	}
}
