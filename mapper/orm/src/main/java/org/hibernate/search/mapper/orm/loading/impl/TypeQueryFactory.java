/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.MultiIdentifierLoadAccess;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.loading.spi.ConditionalExpression;

public interface TypeQueryFactory<E, I> {

	static <E, I> TypeQueryFactory<E, I> create(Class<E> entityClass, String ormEntityName,
			Class<I> uniquePropertyType, String uniquePropertyName,
			boolean uniquePropertyIsTheEntityId) {
		if ( entityClass.equals( Map.class ) ) {
			// This is a dynamic-map entity.
			// They don't have a representation in the JPA metamodel
			// and can't be queried using the Criteria API.
			// Use HQL queries instead, even if it feels a bit dirty.
			return new HqlTypeQueryFactory<>( entityClass, ormEntityName,
					uniquePropertyType, uniquePropertyName,
					uniquePropertyIsTheEntityId );
		}
		else {
			return CriteriaTypeQueryFactory.create( entityClass, uniquePropertyType, uniquePropertyName,
					uniquePropertyIsTheEntityId );
		}
	}

	Query<Long> createQueryForCount(SharedSessionContractImplementor session,
			Set<? extends Class<? extends E>> includedTypesFilter);

	Query<I> createQueryForIdentifierListing(SharedSessionContractImplementor session,
			Set<? extends Class<? extends E>> includedTypesFilter);

	Query<Long> createQueryForCount(SharedSessionContractImplementor session, EntityMappingType entityMappingType,
			Set<? extends Class<? extends E>> includedTypesFilter,
			List<ConditionalExpression> conditionalExpressions);

	Query<I> createQueryForIdentifierListing(SharedSessionContractImplementor session, EntityMappingType entityMappingType,
			Set<? extends Class<? extends E>> includedTypesFilter,
			List<ConditionalExpression> conditionalExpressions, String order);

	Query<E> createQueryForLoadByUniqueProperty(SessionImplementor session, String parameterName);

	MultiIdentifierLoadAccess<E> createMultiIdentifierLoadAccess(SessionImplementor session);

	boolean uniquePropertyIsTheEntityId();

}
