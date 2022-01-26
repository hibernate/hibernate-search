/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.util.Map;
import java.util.Set;

import org.hibernate.MultiIdentifierLoadAccess;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.massindexing.impl.ConditionalExpression;

public interface TypeQueryFactory<E, I> {

	static TypeQueryFactory<?, ?> create(SessionFactoryImplementor sessionFactory, EntityPersister entityPersister,
			String uniquePropertyName) {
		JpaMetamodel metamodel = sessionFactory.getJpaMetamodel();
		EntityDomainType<?> typeOrNull = metamodel.entity( entityPersister.getEntityName() );
		if ( typeOrNull != null && !( entityPersister.getMappedClass().equals( Map.class ) ) ) {
			return CriteriaTypeQueryFactory.create( typeOrNull, uniquePropertyName );
		}
		else {
			// Most likely this is a dynamic-map entity; they don't have a representation in the JPA metamodel
			// and can't be queried using the Criteria API.
			// Use HQL queries instead, even if it feels a bit dirty.
			return new HqlTypeQueryFactory<>( entityPersister, uniquePropertyName );
		}
	}

	Query<Long> createQueryForCount(SharedSessionContractImplementor session,
			Set<? extends Class<? extends E>> includedTypesFilter);

	Query<I> createQueryForIdentifierListing(SharedSessionContractImplementor session,
			Set<? extends Class<? extends E>> includedTypesFilter);

	Query<Long> createQueryForCount(SharedSessionContractImplementor session, EntityPersister persister,
			Set<? extends Class<? extends E>> includedTypesFilter, ConditionalExpression conditionalExpression);

	Query<I> createQueryForIdentifierListing(SharedSessionContractImplementor session, EntityPersister persister,
			Set<? extends Class<? extends E>> includedTypesFilter, ConditionalExpression conditionalExpression);

	Query<E> createQueryForLoadByUniqueProperty(SessionImplementor session, String parameterName);

	MultiIdentifierLoadAccess<E> createMultiIdentifierLoadAccess(SessionImplementor session);

	boolean uniquePropertyIsTheEntityId();

}
