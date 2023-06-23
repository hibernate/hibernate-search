/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.util.Set;

import org.hibernate.MultiIdentifierLoadAccess;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.massindexing.impl.ConditionalExpression;

public class HibernateOrmQueryLoader<E, I> {
	private final TypeQueryFactory<E, I> queryFactory;
	private final Set<Class<? extends E>> includedTypesFilter;
	private final EntityPersister persister;
	private final ConditionalExpression conditionalExpression;

	public HibernateOrmQueryLoader(TypeQueryFactory<E, I> queryFactory,
			Set<Class<? extends E>> includedTypesFilter) {
		this.queryFactory = queryFactory;
		this.includedTypesFilter = includedTypesFilter;
		this.persister = null;
		this.conditionalExpression = null;
	}

	public HibernateOrmQueryLoader(TypeQueryFactory<E, I> queryFactory,
			EntityPersister persister, Set<Class<? extends E>> includedTypesFilter,
			ConditionalExpression conditionalExpression) {
		this.queryFactory = queryFactory;
		this.includedTypesFilter = includedTypesFilter;
		this.persister = persister;
		this.conditionalExpression = conditionalExpression;
	}

	public Query<Long> createCountQuery(SharedSessionContractImplementor session) {
		return ( conditionalExpression == null )
				? queryFactory.createQueryForCount( session, includedTypesFilter )
				: queryFactory.createQueryForCount(
						session, persister, includedTypesFilter, conditionalExpression
				);
	}

	public Query<I> createIdentifiersQuery(SharedSessionContractImplementor session) {
		return ( conditionalExpression == null )
				? queryFactory.createQueryForIdentifierListing( session, includedTypesFilter )
				: queryFactory.createQueryForIdentifierListing(
						session, persister, includedTypesFilter, conditionalExpression
				);
	}

	public Query<E> createLoadingQuery(SessionImplementor session, String idParameterName) {
		return queryFactory.createQueryForLoadByUniqueProperty( session, idParameterName );
	}

	public MultiIdentifierLoadAccess<E> createMultiIdentifierLoadAccess(SessionImplementor session) {
		return queryFactory.createMultiIdentifierLoadAccess( session );
	}

	public boolean uniquePropertyIsTheEntityId() {
		return queryFactory.uniquePropertyIsTheEntityId();
	}
}
