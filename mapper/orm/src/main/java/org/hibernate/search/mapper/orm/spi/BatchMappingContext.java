/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.spi;

import jakarta.persistence.EntityManager;

import org.hibernate.search.mapper.orm.tenancy.spi.TenancyConfiguration;

public interface BatchMappingContext {

	BatchTypeContextProvider typeContextProvider();

	BatchSessionContext sessionContext(EntityManager entityManager);

	<T> BatchScopeContext<T> scope(Class<T> expectedSuperType);

	<T> BatchScopeContext<T> scope(Class<T> expectedSuperType, String entityName);

	TenancyConfiguration tenancyConfiguration();

}
