/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.util.Set;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.Query;

public class HibernateOrmQueryLoader<E, I> {
	private final TypeQueryFactory<E, I> queryFactory;
	private final Set<Class<? extends E>> includedTypesFilter;

	public HibernateOrmQueryLoader(TypeQueryFactory<E, I> queryFactory,
			Set<Class<? extends E>> includedTypesFilter) {
		this.queryFactory = queryFactory;
		this.includedTypesFilter = includedTypesFilter;
	}

	public Query<Long> createCountQuery(SharedSessionContractImplementor session) {
		return queryFactory.createQueryForCount( session, includedTypesFilter );
	}

	public Query<I> createIdentifiersQuery(SharedSessionContractImplementor session) {
		return queryFactory.createQueryForIdentifierListing( session, includedTypesFilter );
	}

	public Query<E> createLoadingQuery(SessionImplementor session, String idParameterName) {
		return queryFactory.createQueryForLoadByUniqueProperty( session, idParameterName );
	}
}
