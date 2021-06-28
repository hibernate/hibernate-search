/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.util.Set;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.Query;

public abstract class ConditionalExpressionQueryFactory<E, I> implements TypeQueryFactory<E, I> {

	protected final String uniquePropertyName;

	public ConditionalExpressionQueryFactory(String uniquePropertyName) {
		this.uniquePropertyName = uniquePropertyName;
	}

	public Query<Long> createQueryForCount(SharedSessionContractImplementor session, EntityPersister persister,
			Set<? extends Class<? extends E>> includedTypesFilter, String conditionalExpression) {
		return createQueryWithConditionalExpression( session,
				"select count(e) from " + persister.getEntityName() + " e",
				Long.class, "e", includedTypesFilter, conditionalExpression
		);
	}

	public Query<I> createQueryForIdentifierListing(SharedSessionContractImplementor session, EntityPersister persister,
			Set<? extends Class<? extends E>> includedTypesFilter, String conditionalExpression) {
		return createQueryWithConditionalExpression( session,
				"select e. " + uniquePropertyName + " from " + persister.getEntityName() + " e",
				(Class<I>) persister.getIdentifierType().getReturnedClass(), "e",
				includedTypesFilter, conditionalExpression
		);
	}

	private <T> Query<T> createQueryWithConditionalExpression(SharedSessionContractImplementor session,
			String hql, Class<T> returnedType, String entityAlias,
			Set<? extends Class<? extends E>> includedTypesFilter, String conditionalExpression) {
		if ( includedTypesFilter.isEmpty() ) {
			return createQueryWithConditionalExpression( session, hql, returnedType, conditionalExpression );
		}

		hql += " where type(" + entityAlias + ") in (:types) and " + conditionalExpression;
		Query<T> query = session.createQuery( hql, returnedType );
		query.setParameterList( "types", includedTypesFilter );
		return query;
	}

	private <T> Query<T> createQueryWithConditionalExpression(SharedSessionContractImplementor session,
			String hql, Class<T> returnedType, String conditionalExpression) {
		hql += " where " + conditionalExpression;
		Query<T> query = session.createQuery( hql, returnedType );
		return query;
	}
}
