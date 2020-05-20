/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.loading.impl;

import java.io.Serializable;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * A lookup strategy that checks the persistence context (first level cache).
 * <p>
 * Ported from Search 5: {@code org.hibernate.search.query.hibernate.impl.PersistenceContextObjectInitializer}.
 *
 * @author Emmanuel Bernard
 */
class PersistenceContextLookupStrategy<E>
		implements EntityLoadingCacheLookupStrategyImplementor<E> {

	static PersistenceContextLookupStrategy<?> create(EntityPersister commonEntitySuperTypePersister,
			SessionImplementor session) {
		return new PersistenceContextLookupStrategy<>(
				commonEntitySuperTypePersister, session
		);
	}

	private final EntityPersister persister;
	private final SessionImplementor session;
	private final PersistenceContext persistenceContext;

	private PersistenceContextLookupStrategy(EntityPersister persister,
			SessionImplementor session) {
		this.persister = persister;
		this.session = session;
		this.persistenceContext = session.getPersistenceContext();
	}

	@Override
	public E lookup(Object entityId) {
		EntityKey entityKey = session.generateEntityKey( (Serializable) entityId, persister );
		/*
		 * The key was obtained from the persister for E,
		 * so the key may only match an instance of E.
		 */
		@SuppressWarnings("unchecked")
		E loadedEntityOrNull = (E) persistenceContext.getEntity( entityKey );
		return loadedEntityOrNull;
	}
}
