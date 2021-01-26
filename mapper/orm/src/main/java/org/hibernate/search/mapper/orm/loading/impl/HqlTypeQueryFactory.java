/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.Query;

class HqlTypeQueryFactory<E> implements TypeQueryFactory<E> {

	private final EntityPersister entityPersister;
	private final String uniquePropertyName;

	HqlTypeQueryFactory(EntityPersister entityPersister, String uniquePropertyName) {
		this.entityPersister = entityPersister;
		this.uniquePropertyName = uniquePropertyName;
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
