/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.util.Set;

import org.hibernate.MultiIdentifierLoadAccess;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.Query;

class HqlTypeQueryFactory<E, I> extends ConditionalExpressionQueryFactory<E, I> {

	private final Class<E> entityClass;
	private final String ormEntityName;

	HqlTypeQueryFactory(Class<E> entityClass, String ormEntityName,
			Class<I> uniquePropertyType, String uniquePropertyName,
			boolean uniquePropertyIsTheEntityId) {
		super( uniquePropertyType, uniquePropertyName, uniquePropertyIsTheEntityId );
		this.entityClass = entityClass;
		this.ormEntityName = ormEntityName;
	}

	@Override
	public Query<Long> createQueryForCount(SharedSessionContractImplementor session,
			Set<? extends Class<? extends E>> includedTypesFilter) {
		return createQueryWithTypesFilter( session,
				"select count(e) from " + ormEntityName + " e",
				Long.class,
				"e", includedTypesFilter );
	}

	@Override
	public Query<I> createQueryForIdentifierListing(SharedSessionContractImplementor session,
			Set<? extends Class<? extends E>> includedTypesFilter) {
		return createQueryWithTypesFilter( session,
				"select e. " + uniquePropertyName + " from " + ormEntityName + " e",
				uniquePropertyType, "e", includedTypesFilter );
	}

	@SuppressWarnings("unchecked")
	@Override
	public Query<E> createQueryForLoadByUniqueProperty(SessionImplementor session, String parameterName) {
		return session.createQuery(
				"select e from " + ormEntityName
						+ " e where " + uniquePropertyName + " in (:" + parameterName + ")",
				entityClass
		);
	}

	@Override
	public MultiIdentifierLoadAccess<E> createMultiIdentifierLoadAccess(SessionImplementor session) {
		return session.byMultipleIds( ormEntityName );
	}

	private <T> Query<T> createQueryWithTypesFilter(SharedSessionContractImplementor session,
			String hql, Class<T> returnedType, String entityAlias,
			Set<? extends Class<? extends E>> includedTypesFilter) {
		if ( !includedTypesFilter.isEmpty() ) {
			hql += " where type(" + entityAlias + ") in (:types)";
		}
		Query<T> query = session.createQuery( hql, returnedType );
		if ( !includedTypesFilter.isEmpty() ) {
			query.setParameterList( "types", includedTypesFilter );
		}
		return query;
	}
}
