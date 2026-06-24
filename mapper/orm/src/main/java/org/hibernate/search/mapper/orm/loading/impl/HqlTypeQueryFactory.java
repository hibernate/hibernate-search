/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.util.List;
import java.util.Set;

import jakarta.persistence.FindOption;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.RootGraph;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.spi.SelectionQueryImplementor;

class HqlTypeQueryFactory<E, I> extends ConditionalExpressionQueryFactory<E, I> {

	private final Class<E> entityClass;
	private final RootGraph<E> rootGraph;
	private final String ormEntityName;

	HqlTypeQueryFactory(RootGraph<E> rootGraph, Class<E> entityClass, String ormEntityName,
			Class<I> uniquePropertyType, String uniquePropertyName,
			boolean uniquePropertyIsTheEntityId) {
		super( uniquePropertyType, uniquePropertyName, uniquePropertyIsTheEntityId );
		this.rootGraph = rootGraph;
		this.entityClass = entityClass;
		this.ormEntityName = ormEntityName;
	}

	@Override
	public SelectionQuery<Long> createQueryForCount(SharedSessionContractImplementor session,
			Set<? extends Class<? extends E>> includedTypesFilter) {
		return createQueryWithTypesFilter( session,
				"select count(e) from " + ormEntityName + " e",
				Long.class,
				"e", includedTypesFilter );
	}

	@Override
	public SelectionQuery<I> createQueryForIdentifierListing(SharedSessionContractImplementor session,
			Set<? extends Class<? extends E>> includedTypesFilter) {
		return createQueryWithTypesFilter( session,
				"select e. " + uniquePropertyName + " from " + ormEntityName + " e",
				uniquePropertyType, "e", includedTypesFilter );
	}

	@Override
	public SelectionQueryImplementor<E> createQueryForLoadByUniqueProperty(SharedSessionContractImplementor session,
			String parameterName) {
		return session.createQuery(
				"select e from " + ormEntityName
						+ " e where " + uniquePropertyName + " in (:" + parameterName + ")",
				entityClass
		);
	}

	@Override
	public List<E> findMultiple(SharedSessionContractImplementor session, List<?> ids, FindOption... options) {
		return session.findMultiple( rootGraph, ids, options );
	}

	private <T> SelectionQuery<T> createQueryWithTypesFilter(SharedSessionContractImplementor session,
			String hql, Class<T> returnedType, String entityAlias,
			Set<? extends Class<? extends E>> includedTypesFilter) {
		if ( !includedTypesFilter.isEmpty() ) {
			hql += " where type(" + entityAlias + ") in (:types)";
		}
		SelectionQuery<T> query = session.createQuery( hql, returnedType );
		if ( !includedTypesFilter.isEmpty() ) {
			query.setParameterList( "types", includedTypesFilter );
		}
		return query;
	}
}
