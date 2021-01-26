/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class HqlTypeQueryFactory<E, I> implements TypeQueryFactory<E, I> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final EntityPersister entityPersister;
	private final String uniquePropertyName;

	HqlTypeQueryFactory(EntityPersister entityPersister, String uniquePropertyName) {
		this.entityPersister = entityPersister;
		this.uniquePropertyName = uniquePropertyName;
	}

	@Override
	public Query<Long> createQueryForCount(SharedSessionContractImplementor session,
			Set<? extends Class<? extends E>> includedTypesFilter) {
		// TODO HSEARCH-3771 Mass indexing for ORM's dynamic-map entity types
		throw log.nonJpaEntityType( entityPersister.getEntityName() );
	}

	@Override
	public Query<I> createQueryForIdentifierListing(SharedSessionContractImplementor session,
			Set<? extends Class<? extends E>> includedTypesFilter) {
		// TODO HSEARCH-3771 Mass indexing for ORM's dynamic-map entity types
		throw log.nonJpaEntityType( entityPersister.getEntityName() );
	}

	@SuppressWarnings("unchecked")
	@Override
	public Query<E> createQueryForLoadByUniqueProperty(SessionImplementor session, String parameterName) {
		return session.createQuery(
				"select e from " + entityPersister.getEntityName()
						+ " e where " + uniquePropertyName + " in (:" + parameterName + ")",
				(Class<E>) entityPersister.getMappedClass()
		);
	}
}
