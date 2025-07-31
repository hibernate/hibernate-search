/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.util.Map;

import org.hibernate.ObjectNotFoundException;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.graph.RootGraph;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.search.mapper.orm.logging.impl.OrmMiscLog;
import org.hibernate.search.util.common.annotation.impl.SuppressForbiddenApis;

/**
 * A lookup strategy that checks the persistence context (first level cache),
 * then the second level cache.
 * <p>
 * Ported from Search 5: {@code org.hibernate.search.query.hibernate.impl.SecondLevelCacheObjectInitializer}.
 *
 * @author Emmanuel Bernard
 */
@SuppressForbiddenApis(reason = "EntityPersister is needed to retrieve/use EntityDataAccess")
abstract class PersistenceContextThenSecondLevelCacheLookupStrategy
		implements EntityLoadingCacheLookupStrategyImplementor {

	static EntityLoadingCacheLookupStrategyImplementor create(EntityMappingType entityMappingType,
			SessionImplementor session) {
		EntityLoadingCacheLookupStrategyImplementor persistenceContextLookupStrategy =
				PersistenceContextLookupStrategy.create( session );
		EntityPersister entityPersister = entityMappingType.getEntityPersister();
		EntityDataAccess cacheAccess = entityPersister.getCacheAccessStrategy();
		if ( cacheAccess == null ) {
			// No second-level cache
			OrmMiscLog.INSTANCE.skippingSecondLevelCacheLookupsForNonCachedEntityTypeEntityLoader(
					entityPersister.getEntityName() );
			return persistenceContextLookupStrategy;
		}

		if ( RepresentationMode.MAP.equals( entityMappingType.getRepresentationStrategy().getMode() ) ) {
			return new DynamicMapPersistenceContextThenSecondLevelCacheLookupStrategy(
					persistenceContextLookupStrategy,
					entityPersister,
					cacheAccess,
					session
			);
		}
		else {
			return new PojoPersistenceContextThenSecondLevelCacheLookupStrategy(
					persistenceContextLookupStrategy,
					entityPersister,
					cacheAccess,
					session
			);
		}
	}

	private final EntityLoadingCacheLookupStrategyImplementor persistenceContextLookupStrategy;
	private final EntityDataAccess cacheAccess;
	protected final EntityPersister persister;
	protected final SessionImplementor session;

	private PersistenceContextThenSecondLevelCacheLookupStrategy(
			EntityLoadingCacheLookupStrategyImplementor persistenceContextLookupStrategy,
			EntityPersister persister,
			EntityDataAccess cacheAccess,
			SessionImplementor session) {
		this.persistenceContextLookupStrategy = persistenceContextLookupStrategy;
		this.persister = persister;
		this.cacheAccess = cacheAccess;
		this.session = session;
	}

	@Override
	public Object lookup(EntityKey entityKey) {
		// Try the persistence context first, because it's faster
		Object fromPersistenceContext = persistenceContextLookupStrategy.lookup( entityKey );
		if ( fromPersistenceContext != null ) {
			return fromPersistenceContext;
		}

		/*
		 * Note we must call this method specifically,
		 * and not sessionFactory.getCache().containsEntity() which is unaware of the session
		 * and thus cannot take the tenant identifier into account.
		 */
		final Object key = cacheAccess.generateCacheKey(
				entityKey.getIdentifier(), persister, session.getSessionFactory(), session.getTenantIdentifier()
		);

		if ( !cacheAccess.contains( key ) ) {
			return null;
		}

		try {
			// This will load the object from the second level cache
			return lookupByIdentifier( entityKey );
		}
		catch (ObjectNotFoundException ignored) {
			// Unlikely but needed: an index might be out of sync, and the cache might be as well
			// Ignore the exception and handle as a cache miss by returning null
			return null;
		}
	}

	protected abstract Object lookupByIdentifier(EntityKey entityKey);

	@SuppressForbiddenApis(reason = "EntityPersister is needed to retrieve/use EntityDataAccess")
	private static class DynamicMapPersistenceContextThenSecondLevelCacheLookupStrategy
			extends PersistenceContextThenSecondLevelCacheLookupStrategy {

		private final RootGraph<Map<String, ?>> graphForDynamicEntity;

		private DynamicMapPersistenceContextThenSecondLevelCacheLookupStrategy(
				EntityLoadingCacheLookupStrategyImplementor persistenceContextLookupStrategy, EntityPersister persister,
				EntityDataAccess cacheAccess, SessionImplementor session) {
			super( persistenceContextLookupStrategy, persister, cacheAccess, session );
			this.graphForDynamicEntity = session.getSessionFactory().createGraphForDynamicEntity( persister.getEntityName() );
		}

		@Override
		protected Object lookupByIdentifier(EntityKey entityKey) {
			return session.find( graphForDynamicEntity, entityKey.getIdentifier() );
		}
	}

	@SuppressForbiddenApis(reason = "EntityPersister is needed to retrieve/use EntityDataAccess")
	private static class PojoPersistenceContextThenSecondLevelCacheLookupStrategy
			extends PersistenceContextThenSecondLevelCacheLookupStrategy {

		private PojoPersistenceContextThenSecondLevelCacheLookupStrategy(
				EntityLoadingCacheLookupStrategyImplementor persistenceContextLookupStrategy, EntityPersister persister,
				EntityDataAccess cacheAccess, SessionImplementor session) {
			super( persistenceContextLookupStrategy, persister, cacheAccess, session );
		}

		@Override
		protected Object lookupByIdentifier(EntityKey entityKey) {
			return session.find( persister.getMappedClass(), entityKey.getIdentifier() );
		}
	}
}
