/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.util.Optional;

import org.hibernate.CacheMode;
import org.hibernate.search.mapper.orm.loading.spi.ConditionalExpression;
import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmLoadingMappingContext;
import org.hibernate.search.mapper.orm.tenancy.spi.TenancyConfiguration;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingTypeContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingContext;

public interface HibernateOrmMassLoadingContext extends PojoMassIndexingContext {

	HibernateOrmLoadingMappingContext mapping();

	/**
	 * @return the transaction timeout
	 */
	Integer idLoadingTransactionTimeout();

	/**
	 * @return the {@link CacheMode}
	 */
	CacheMode cacheMode();

	/**
	 * @return how many entities to load and index in each batch.
	 */
	int objectLoadingBatchSize();

	/**
	 * @return the objects limit used to load the root entities.
	 */
	long objectsLimit();

	/**
	 * @return fetch size used to load the root entities.
	 */
	int idFetchSize();

	/**
	 * @return The conditional expression to apply when loading the given type,
	 * inherited from supertypes by default,
	 * or {@link Optional#empty()} if there is no condition to apply.
	 */
	Optional<ConditionalExpression> conditionalExpression(PojoLoadingTypeContext<?> typeContext);

	TenancyConfiguration tenancyConfiguration();
}
