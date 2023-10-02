/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.util.List;
import java.util.Set;

import org.hibernate.MultiIdentifierLoadAccess;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.loading.spi.ConditionalExpression;
import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmQueryLoader;

class HibernateOrmQueryLoaderImpl<E, I> implements HibernateOrmQueryLoader<E, I> {
	private final TypeQueryFactory<E, I> queryFactory;
	private final Set<Class<? extends E>> includedTypesFilter;
	private final EntityDomainType<?> entityDomainType;
	private final List<ConditionalExpression> conditionalExpressions;
	private final String order;

	public HibernateOrmQueryLoaderImpl(TypeQueryFactory<E, I> queryFactory,
			Set<Class<? extends E>> includedTypesFilter) {
		this.queryFactory = queryFactory;
		this.includedTypesFilter = includedTypesFilter;
		this.entityDomainType = null;
		this.conditionalExpressions = List.of();
		this.order = null;
	}

	public HibernateOrmQueryLoaderImpl(TypeQueryFactory<E, I> queryFactory,
			EntityDomainType<?> entityDomainType, Set<Class<? extends E>> includedTypesFilter,
			List<ConditionalExpression> conditionalExpressions, String order) {
		this.queryFactory = queryFactory;
		this.includedTypesFilter = includedTypesFilter;
		this.entityDomainType = entityDomainType;
		this.conditionalExpressions = conditionalExpressions;
		this.order = order;
	}

	@Override
	public Query<Long> createCountQuery(SharedSessionContractImplementor session) {
		return conditionalExpressions.isEmpty()
				? queryFactory.createQueryForCount( session, includedTypesFilter )
				: queryFactory.createQueryForCount( session, entityDomainType, includedTypesFilter, conditionalExpressions );
	}

	@Override
	public Query<I> createIdentifiersQuery(SharedSessionContractImplementor session) {
		return conditionalExpressions.isEmpty() && order == null
				? queryFactory.createQueryForIdentifierListing( session, includedTypesFilter )
				: queryFactory.createQueryForIdentifierListing( session, entityDomainType, includedTypesFilter,
						conditionalExpressions, order );
	}

	@Override
	public Query<E> createLoadingQuery(SessionImplementor session, String idParameterName) {
		return queryFactory.createQueryForLoadByUniqueProperty( session, idParameterName );
	}

	@Override
	public MultiIdentifierLoadAccess<E> createMultiIdentifierLoadAccess(SessionImplementor session) {
		return queryFactory.createMultiIdentifierLoadAccess( session );
	}

	@Override
	public boolean uniquePropertyIsTheEntityId() {
		return queryFactory.uniquePropertyIsTheEntityId();
	}
}
