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
import jakarta.persistence.metamodel.SingularAttribute;

import org.hibernate.MultiIdentifierLoadAccess;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.Query;

class CriteriaTypeQueryFactory<E, I> extends ConditionalExpressionQueryFactory<E, I> {

	public static <E> CriteriaTypeQueryFactory<E, ?> create(EntityDomainType<E> type,
			String uniquePropertyName) {
		return new CriteriaTypeQueryFactory<>( type, type.getSingularAttribute( uniquePropertyName ) );
	}

	private final EntityDomainType<E> type;
	private final SingularAttribute<? super E, I> uniqueProperty;

	private CriteriaTypeQueryFactory(EntityDomainType<E> type, SingularAttribute<? super E, I> uniqueProperty) {
		super( uniqueProperty.getJavaType(), uniqueProperty.getName() );
		this.type = type;
		this.uniqueProperty = uniqueProperty;
	}

	@Override
	public Query<Long> createQueryForCount(SharedSessionContractImplementor session,
			Set<? extends Class<? extends E>> includedTypesFilter) {
		CriteriaBuilder criteriaBuilder = session.getFactory().getCriteriaBuilder();
		CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery( Long.class );
		Root<E> root = criteriaQuery.from( type );
		criteriaQuery.select( criteriaBuilder.count( root ) );
		if ( !includedTypesFilter.isEmpty() ) {
			criteriaQuery.where( root.type().in( includedTypesFilter ) );
		}
		return session.createQuery( criteriaQuery );
	}

	@Override
	public Query<I> createQueryForIdentifierListing(SharedSessionContractImplementor session,
			Set<? extends Class<? extends E>> includedTypesFilter) {
		CriteriaBuilder criteriaBuilder = session.getFactory().getCriteriaBuilder();
		CriteriaQuery<I> criteriaQuery = criteriaBuilder.createQuery( uniqueProperty.getJavaType() );
		Root<E> root = criteriaQuery.from( type );
		Path<I> idPath = root.get( uniqueProperty );
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
		CriteriaQuery<E> criteriaQuery = criteriaBuilder.createQuery( type.getJavaType() );
		Root<E> root = criteriaQuery.from( type );
		Path<?> uniquePropertyInRoot = root.get( uniqueProperty );
		criteriaQuery.where( uniquePropertyInRoot.in( idsParameter ) );
		return session.createQuery( criteriaQuery );
	}

	@Override
	public MultiIdentifierLoadAccess<E> createMultiIdentifierLoadAccess(SessionImplementor session) {
		return session.byMultipleIds( type.getJavaType() );
	}

	@Override
	public boolean uniquePropertyIsTheEntityId() {
		return uniqueProperty.isId();
	}
}
