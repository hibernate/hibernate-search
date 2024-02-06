/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.util.Collection;
import java.util.Set;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import org.hibernate.MultiIdentifierLoadAccess;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.Query;

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
	public Query<Long> createQueryForCount(SharedSessionContractImplementor session,
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
	public Query<I> createQueryForIdentifierListing(SharedSessionContractImplementor session,
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
	@SuppressWarnings("rawtypes")
	public Query<E> createQueryForLoadByUniqueProperty(SessionImplementor session, String parameterName) {
		CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
		ParameterExpression<Collection> idsParameter = criteriaBuilder.parameter( Collection.class, parameterName );
		CriteriaQuery<E> criteriaQuery = criteriaBuilder.createQuery( entityClass );
		Root<E> root = criteriaQuery.from( entityClass );
		Path<?> uniquePropertyInRoot = root.get( uniquePropertyName );
		criteriaQuery.where( uniquePropertyInRoot.in( idsParameter ) );
		return session.createQuery( criteriaQuery );
	}

	@Override
	public MultiIdentifierLoadAccess<E> createMultiIdentifierLoadAccess(SessionImplementor session) {
		return session.byMultipleIds( entityClass );
	}
}
