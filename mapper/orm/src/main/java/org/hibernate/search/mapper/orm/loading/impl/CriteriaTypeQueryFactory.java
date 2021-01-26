/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.util.Collection;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.query.Query;

class CriteriaTypeQueryFactory<E> implements TypeQueryFactory<E> {

	public static <E> CriteriaTypeQueryFactory<E> create(EntityTypeDescriptor<E> typeDescriptor,
			String uniquePropertyName) {
		return new CriteriaTypeQueryFactory<>( typeDescriptor,
				typeDescriptor.getSingularAttribute( uniquePropertyName ) );
	}

	private final EntityTypeDescriptor<E> typeDescriptor;
	private final SingularAttribute<? super E, ?> uniqueProperty;

	private CriteriaTypeQueryFactory(EntityTypeDescriptor<E> typeDescriptor,
			SingularAttribute<? super E, ?> uniqueProperty) {
		this.typeDescriptor = typeDescriptor;
		this.uniqueProperty = uniqueProperty;
	}

	@Override
	public Query<E> createQueryForLoadByUniqueProperty(SessionImplementor session, String parameterName) {
		CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
		ParameterExpression<Collection> idsParameter = criteriaBuilder.parameter( Collection.class, parameterName );
		CriteriaQuery<E> criteriaQuery = criteriaBuilder.createQuery( typeDescriptor.getJavaType() );
		Root<E> root = criteriaQuery.from( typeDescriptor );
		Path<?> uniquePropertyInRoot = root.get( uniqueProperty );
		criteriaQuery.where( uniquePropertyInRoot.in( idsParameter ) );
		return session.createQuery( criteriaQuery );
	}
}
