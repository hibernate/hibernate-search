/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.spi;

import java.util.Collection;

import jakarta.persistence.EntityManager;

import org.hibernate.search.mapper.orm.tenancy.spi.TenancyConfiguration;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingDefaultCleanOperation;

public interface BatchMappingContext {

	BatchTypeContextProvider typeContextProvider();

	BatchSessionContext sessionContext(EntityManager entityManager);

	<SR, T> BatchScopeContext<T> scope(Class<T> expectedSuperType);

	<SR, T> BatchScopeContext<T> scope(Class<T> expectedSuperType, String entityName);

	<SR, T> BatchScopeContext<T> scope(Collection<? extends Class<? extends T>> classes);

	TenancyConfiguration tenancyConfiguration();

	MassIndexingDefaultCleanOperation massIndexingDefaultCleanOperation();
}
