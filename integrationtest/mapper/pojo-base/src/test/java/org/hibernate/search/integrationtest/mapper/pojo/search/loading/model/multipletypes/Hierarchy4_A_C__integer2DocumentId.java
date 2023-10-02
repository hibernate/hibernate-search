/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.multipletypes;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.PersistenceTypeKey;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;

@SearchEntity(name = Hierarchy4_A_C__integer2DocumentId.NAME)
@Indexed
public class Hierarchy4_A_C__integer2DocumentId extends Hierarchy4_A__NonAbstract_NonIndexed {

	public static final String NAME = "H4_A_C";
	public static final PersistenceTypeKey<Hierarchy4_A_C__integer2DocumentId, Integer> PERSISTENCE_KEY =
			new PersistenceTypeKey<>( Hierarchy4_A_C__integer2DocumentId.class, Integer.class );

	@DocumentId
	private int integer2;

	public Hierarchy4_A_C__integer2DocumentId(int id, int integer2) {
		super( id );
		this.integer2 = integer2;
	}
}
