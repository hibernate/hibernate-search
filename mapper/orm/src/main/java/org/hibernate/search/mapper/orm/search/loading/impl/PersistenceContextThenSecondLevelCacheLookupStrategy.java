/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.loading.impl;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;

import org.hibernate.ObjectNotFoundException;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A lookup strategy that checks the persistence context (first level cache),
 * then the second level cache.
 * <p>
 * Ported from Search 5: {@code org.hibernate.search.query.hibernate.impl.SecondLevelCacheObjectInitializer}.
 *
 * @author Emmanuel Bernard
 */
class PersistenceContextThenSecondLevelCacheLookupStrategy<E>
		implements EntityLoadingCacheLookupStrategyImplementor<E> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	static <E> EntityLoadingCacheLookupStrategyImplementor<E> create(EntityTypeDescriptor<E> commonEntitySuperType,
			SessionImplementor session) {
		EntityLoadingCacheLookupStrategyImplementor<E> persistenceContextLookupStrategy =
				PersistenceContextLookupStrategy.create( commonEntitySuperType, session );
		MetamodelImplementor metamodelImplementor = session.getSessionFactory().getMetamodel();
		// All entities with a common entity supertype share the same persister and cache access, so this is safe
		EntityPersister persister = metamodelImplementor.entityPersister( commonEntitySuperType.getTypeName() );
		EntityDataAccess cacheAccess = persister.getCacheAccessStrategy();
		if ( cacheAccess == null ) {
			// No second-level cache
			log.skippingSecondLevelCacheLookupsForNonCachedEntityTypeEntityLoader( commonEntitySuperType.getName() );
			return persistenceContextLookupStrategy;
		}
		return new PersistenceContextThenSecondLevelCacheLookupStrategy<>(
				persistenceContextLookupStrategy,
				commonEntitySuperType,
				persister,
				cacheAccess,
				session
		);
	}

	private final EntityLoadingCacheLookupStrategyImplementor<E> persistenceContextLookupStrategy;
	private final EntityTypeDescriptor<E> commonEntitySuperType;
	private final EntityPersister persister;
	private final EntityDataAccess cacheAccess;
	private final SessionImplementor session;

	private PersistenceContextThenSecondLevelCacheLookupStrategy(
			EntityLoadingCacheLookupStrategyImplementor<E> persistenceContextLookupStrategy,
			EntityTypeDescriptor<E> commonEntitySuperType,
			EntityPersister persister,
			EntityDataAccess cacheAccess,
			SessionImplementor session) {
		this.persistenceContextLookupStrategy = persistenceContextLookupStrategy;
		this.commonEntitySuperType = commonEntitySuperType;
		this.persister = persister;
		this.cacheAccess = cacheAccess;
		this.session = session;
	}

	@Override
	@SuppressWarnings("unchecked") // By contract,
	public E lookup(Object entityId) {
		// Try the persistence context first, because it's faster
		E fromPersistenceContext = persistenceContextLookupStrategy.lookup( entityId );
		if ( fromPersistenceContext != null ) {
			return fromPersistenceContext;
		}

		if ( cacheAccess == null ) {
			// This type is not cached.
			return null;
		}

		Serializable serializableEntityId = (Serializable) entityId;

		/*
		 * Note we must call this method specifically,
		 * and not sessionFactory.getCache().containsEntity() which is unaware of the session
		 * and thus cannot take the tenant identifier into account.
		 */
		final Object key = cacheAccess.generateCacheKey(
				serializableEntityId, persister, session.getSessionFactory(), session.getTenantIdentifier()
		);

		if ( !cacheAccess.contains( key ) ) {
			return null;
		}

		try {
			// This will load the object from the second level cache
			return (E) session.get( commonEntitySuperType.getTypeName(), serializableEntityId );
		}
		catch (ObjectNotFoundException ignored) {
			// Unlikely but needed: an index might be out of sync, and the cache might be as well
			// Ignore the exception and handle as a cache miss by returning null
			return null;
		}
	}
}
