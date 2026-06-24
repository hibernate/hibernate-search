/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import jakarta.persistence.FindOption;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.spi.SelectionQueryImplementor;

class CriteriaTypeQueryFactory<E, I> extends ConditionalExpressionQueryFactory<E, I> {

	public static <E, I> CriteriaTypeQueryFactory<E, I> create(Class<E> entityClass,
			Class<I> uniquePropertyType, String uniquePropertyName,
			boolean uniquePropertyIsTheEntityId) {
		return new CriteriaTypeQueryFactory<>( entityClass, uniquePropertyType, uniquePropertyName,
				uniquePropertyIsTheEntityId );
	}

	private final Class<E> entityClass;

	private CriteriaTypeQueryFactory(Class<E> entityClass,
			Class<I> uniquePropertyType, String uniquePropertyName,
			boolean uniquePropertyIsTheEntityId) {
		super( uniquePropertyType, uniquePropertyName, uniquePropertyIsTheEntityId );
		this.entityClass = entityClass;
	}

	@Override
	public SelectionQuery<Long> createQueryForCount(SharedSessionContractImplementor session,
			Set<? extends Class<? extends E>> includedTypesFilter) {
		CriteriaBuilder criteriaBuilder = session.getFactory().getCriteriaBuilder();
		CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery( Long.class );
		Root<E> root = criteriaQuery.from( entityClass );
		criteriaQuery.select( criteriaBuilder.count( root ) );
		if ( !includedTypesFilter.isEmpty() ) {
			criteriaQuery.where( root.type().in( includedTypesFilter ) );
		}
		return session.createQuery( criteriaQuery );
	}

	@Override
	public SelectionQuery<I> createQueryForIdentifierListing(SharedSessionContractImplementor session,
			Set<? extends Class<? extends E>> includedTypesFilter) {
		CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
		CriteriaQuery<I> criteriaQuery = criteriaBuilder.createQuery( uniquePropertyType );
		Root<E> root = criteriaQuery.from( entityClass );
		Path<I> idPath = root.get( uniquePropertyName );
		criteriaQuery.select( idPath );
		if ( !includedTypesFilter.isEmpty() ) {
			criteriaQuery.where( root.type().in( includedTypesFilter ) );
		}
		return session.createQuery( criteriaQuery );
	}

	@Override
	@SuppressWarnings({ "rawtypes" })
	public SelectionQueryImplementor<E> createQueryForLoadByUniqueProperty(SharedSessionContractImplementor session,
			String parameterName) {
		CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
		ParameterExpression<Collection> idsParameter = criteriaBuilder.parameter( Collection.class, parameterName );
		CriteriaQuery<E> criteriaQuery = criteriaBuilder.createQuery( entityClass );
		Root<E> root = criteriaQuery.from( entityClass );
		Path<?> uniquePropertyInRoot = root.get( uniquePropertyName );
		criteriaQuery.where( uniquePropertyInRoot.in( idsParameter ) );
		return session.createQuery( criteriaQuery );
	}

	@Override
	public List<E> findMultiple(SharedSessionContractImplementor session, List<?> ids, FindOption... options) {
		return session.findMultiple( entityClass, ids, options );
	}
}
