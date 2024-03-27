/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.scope.impl;

import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScopeBuilder;
import org.hibernate.search.mapper.pojo.massindexing.impl.PojoMassIndexingIndexedTypeContext;
import org.hibernate.search.mapper.pojo.schema.management.impl.PojoSchemaManagementIndexedTypeContext;
import org.hibernate.search.mapper.pojo.search.loading.impl.PojoSearchLoadingIndexedTypeContext;
import org.hibernate.search.mapper.pojo.work.impl.PojoWorkIndexedTypeContext;

/**
 * @param <I> The identifier type for the mapped entity type.
 * @param <E> The entity type mapped to the index.
 */
public interface PojoScopeIndexedTypeContext<I, E>
		extends PojoWorkIndexedTypeContext<I, E>, PojoSchemaManagementIndexedTypeContext,
		PojoSearchLoadingIndexedTypeContext<E>, PojoMassIndexingIndexedTypeContext<E> {

	<R, E2> MappedIndexScopeBuilder<R, E2> createScopeBuilder(BackendMappingContext mappingContext);

	void addTo(MappedIndexScopeBuilder<?, ?> builder);

}
