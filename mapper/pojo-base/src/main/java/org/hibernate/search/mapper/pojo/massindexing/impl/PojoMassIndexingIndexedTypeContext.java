/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import java.util.function.Supplier;

import org.hibernate.search.mapper.pojo.identity.impl.IdentifierMappingImplementor;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingTypeContext;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;

/**
 * @param <E> The entity type mapped to the index.
 */
public interface PojoMassIndexingIndexedTypeContext<E> extends PojoLoadingTypeContext<E> {

	Supplier<E> toEntitySupplier(PojoWorkSessionContext sessionContext, Object entity);

	IdentifierMappingImplementor<?, E> identifierMapping();

}
