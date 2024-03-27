/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.singletype;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.PersistenceTypeKey;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.StubEntityLoadingBinder;
import org.hibernate.search.mapper.pojo.loading.mapping.annotation.EntityLoadingBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;

@SearchEntity(name = BasicIndexedEntity.NAME,
		loadingBinder = @EntityLoadingBinderRef(type = StubEntityLoadingBinder.class))
@Indexed
public class BasicIndexedEntity {

	public static final String NAME = "indexed";
	public static final PersistenceTypeKey<BasicIndexedEntity, Integer> PERSISTENCE_KEY =
			new PersistenceTypeKey<>( BasicIndexedEntity.class, Integer.class );

	@DocumentId
	private Integer id;

	protected BasicIndexedEntity() {
	}

	public BasicIndexedEntity(int id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + id + "]";
	}

	public Integer getId() {
		return id;
	}

}
