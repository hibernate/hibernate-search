/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.multipletypes;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.PersistenceTypeKey;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.StubEntityLoadingBinder;
import org.hibernate.search.mapper.pojo.loading.mapping.annotation.EntityLoadingBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;

@SearchEntity(name = Hierarchy5_A__Abstract.NAME,
		loadingBinder = @EntityLoadingBinderRef(type = StubEntityLoadingBinder.class))
public abstract class Hierarchy5_A__Abstract {

	public static final String NAME = "H5_A";
	public static final PersistenceTypeKey<Hierarchy5_A__Abstract, Integer> PERSISTENCE_KEY =
			new PersistenceTypeKey<>( Hierarchy5_A__Abstract.class, Integer.class );

	@DocumentId
	private Integer id;

	public Hierarchy5_A__Abstract(int id) {
		this.id = id;
	}

	public Integer getId() {
		return id;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + id + "]";
	}
}
