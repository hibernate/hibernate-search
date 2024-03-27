/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import java.util.concurrent.CompletableFuture;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.impl.HibernateOrmMapping;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.BackendMappingHandle;

public class HibernateOrmMappingHandle implements BackendMappingHandle {
	private final EntityManagerFactory entityManagerFactory;

	public HibernateOrmMappingHandle(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	@Override
	public CompletableFuture<?> backgroundIndexingCompletion() {
		HibernateOrmMapping mapping = mappingOrNull();
		if ( mapping == null ) {
			return CompletableFuture.completedFuture( null );
		}
		return mapping.backgroundIndexingCompletion();
	}

	// Retrieve the mapping lazily, because Hibernate Search startup may be delayed (e.g. because of CDI).
	private HibernateOrmMapping mappingOrNull() {
		if ( !entityManagerFactory.isOpen() ) {
			return null;
		}
		return (HibernateOrmMapping) Search.mapping( entityManagerFactory );
	}
}
