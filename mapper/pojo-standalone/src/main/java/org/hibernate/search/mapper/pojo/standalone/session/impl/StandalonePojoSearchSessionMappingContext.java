/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.session.impl;

import java.util.Collection;

import org.hibernate.search.mapper.pojo.session.spi.PojoSearchSessionMappingContext;
import org.hibernate.search.mapper.pojo.standalone.massindexing.impl.StandalonePojoMassIndexingMappingContext;
import org.hibernate.search.mapper.pojo.standalone.scope.impl.SearchScopeImpl;
import org.hibernate.search.mapper.pojo.standalone.scope.impl.StandalonePojoScopeMappingContext;
import org.hibernate.search.mapper.pojo.standalone.tenancy.impl.TenancyConfiguration;

public interface StandalonePojoSearchSessionMappingContext
		extends PojoSearchSessionMappingContext, StandalonePojoScopeMappingContext,
		StandalonePojoMassIndexingMappingContext {

	<T> SearchScopeImpl<T> createScope(Collection<? extends Class<? extends T>> types);

	<T> SearchScopeImpl<T> createScope(Class<T> expectedSuperType, Collection<String> entityNames);

	TenancyConfiguration tenancyConfiguration();
}
