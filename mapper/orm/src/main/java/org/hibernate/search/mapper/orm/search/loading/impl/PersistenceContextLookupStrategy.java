/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.loading.impl;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * A lookup strategy that checks the persistence context (first level cache).
 * <p>
 * Ported from Search 5: {@code org.hibernate.search.query.hibernate.impl.PersistenceContextObjectInitializer}.
 *
 * @author Emmanuel Bernard
 */
class PersistenceContextLookupStrategy
		implements EntityLoadingCacheLookupStrategyImplementor {

	static PersistenceContextLookupStrategy create(SessionImplementor session) {
		return new PersistenceContextLookupStrategy( session );
	}

	private final PersistenceContext persistenceContext;

	private PersistenceContextLookupStrategy(SessionImplementor session) {
		this.persistenceContext = session.getPersistenceContext();
	}

	@Override
	public Object lookup(EntityKey entityKey) {
		return persistenceContext.getEntity( entityKey );
	}
}
