/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.hibernate.CacheMode;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmMassLoadingContext;
import org.hibernate.search.mapper.orm.loading.spi.ConditionalExpression;
import org.hibernate.search.mapper.orm.tenancy.spi.TenancyConfiguration;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingTypeContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingContext;

public final class HibernateOrmMassIndexingContext
		implements PojoMassIndexingContext, HibernateOrmMassLoadingContext {

	private final HibernateOrmMassIndexingMappingContext mapping;
	private final Map<Class<?>, ConditionalExpression> conditionalExpressions = new HashMap<>();
	private CacheMode cacheMode = CacheMode.IGNORE;
	private Integer idLoadingTransactionTimeout;
	private int idFetchSize = 100; //reasonable default as we only load IDs
	private int objectLoadingBatchSize = 10;
	private long objectsLimit = 0; //means no limit at all

	public HibernateOrmMassIndexingContext(HibernateOrmMassIndexingMappingContext mapping) {
		this.mapping = mapping;
	}

	@Override
	public HibernateOrmMassIndexingMappingContext mapping() {
		return mapping;
	}

	public void idLoadingTransactionTimeout(int timeoutInSeconds) {
		this.idLoadingTransactionTimeout = timeoutInSeconds;
	}

	@Override
	public Integer idLoadingTransactionTimeout() {
		return idLoadingTransactionTimeout;
	}

	public void cacheMode(CacheMode cacheMode) {
		this.cacheMode = cacheMode;
	}

	@Override
	public CacheMode cacheMode() {
		return cacheMode;
	}

	public void objectLoadingBatchSize(int batchSize) {
		if ( batchSize < 1 ) {
			throw new IllegalArgumentException( "batchSize must be at least 1" );
		}
		this.objectLoadingBatchSize = batchSize;
	}

	@Override
	public int objectLoadingBatchSize() {
		return objectLoadingBatchSize;
	}

	public void objectsLimit(long maximum) {
		this.objectsLimit = maximum;
	}

	@Override
	public long objectsLimit() {
		return objectsLimit;
	}

	public void idFetchSize(int idFetchSize) {
		// don't check for positive/zero values as it's actually used by some databases
		// as special values which might be useful.
		this.idFetchSize = idFetchSize;
	}

	@Override
	public int idFetchSize() {
		return idFetchSize;
	}

	ConditionalExpression reindexOnly(Class<?> type, String conditionalExpression) {
		ConditionalExpression expression = new ConditionalExpression( conditionalExpression );
		conditionalExpressions.put( type, expression );
		return expression;
	}

	@Override
	public Optional<ConditionalExpression> conditionalExpression(PojoLoadingTypeContext<?> typeContext) {
		if ( conditionalExpressions.isEmpty() ) {
			return Optional.empty();
		}

		return typeContext.ascendingSuperTypes().stream()
				.map( typeId -> conditionalExpressions.get( typeId.javaClass() ) )
				.filter( Objects::nonNull )
				.findFirst();
	}

	@Override
	public TenancyConfiguration tenancyConfiguration() {
		return mapping.tenancyConfiguration();
	}

}
