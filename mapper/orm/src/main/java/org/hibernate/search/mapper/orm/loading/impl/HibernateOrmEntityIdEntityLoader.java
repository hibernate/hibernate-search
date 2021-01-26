/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.QueryTimeoutException;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.jpa.QueryHints;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.engine.common.timing.spi.Deadline;
import org.hibernate.search.mapper.orm.search.loading.impl.HibernateOrmComposableSearchEntityLoader;
import org.hibernate.search.mapper.orm.search.loading.impl.EntityGraphHint;
import org.hibernate.search.mapper.orm.search.loading.impl.MutableEntityLoadingOptions;

/**
 * An entity loader for indexed entities whose document ID is the entity ID.
 *
 * @param <E> The type of loaded entities.
 */
class HibernateOrmEntityIdEntityLoader<E> implements HibernateOrmComposableSearchEntityLoader<E> {

	private static final String IDS_PARAMETER_NAME = "ids";

	private final EntityPersister entityPersister;
	private final TypeQueryFactory<? super E> queryFactory;
	private final SessionImplementor session;
	private final PersistenceContextLookupStrategy persistenceContextLookup;
	private final EntityLoadingCacheLookupStrategyImplementor cacheLookupStrategyImplementor;
	private final MutableEntityLoadingOptions loadingOptions;

	HibernateOrmEntityIdEntityLoader(EntityPersister entityPersister, TypeQueryFactory<E> queryFactory,
			SessionImplementor session,
			PersistenceContextLookupStrategy persistenceContextLookup,
			EntityLoadingCacheLookupStrategyImplementor cacheLookupStrategyImplementor,
			MutableEntityLoadingOptions loadingOptions) {
		this.entityPersister = entityPersister;
		this.queryFactory = queryFactory;
		this.session = session;
		this.persistenceContextLookup = persistenceContextLookup;
		this.cacheLookupStrategyImplementor = cacheLookupStrategyImplementor;
		this.loadingOptions = loadingOptions;
	}

	@Override
	public List<E> loadBlocking(List<EntityReference> references, Deadline deadline) {
		if ( cacheLookupStrategyImplementor == null ) {
			Long timeout = deadline == null ? null : deadline.remainingTimeMillis();
			// Optimization: if we don't need to look up the cache, we don't need a map to store intermediary results.
			try {
				return doLoadEntities( references, timeout );
			}
			catch (QueryTimeoutException | javax.persistence.QueryTimeoutException | LockTimeoutException |
					javax.persistence.LockTimeoutException e) {
				if ( deadline == null ) {
					// ORM-initiated timeout: just propagate the exception.
					throw e;
				}
				throw deadline.forceTimeoutAndCreateException( e );
			}
		}
		else {
			return HibernateOrmComposableSearchEntityLoader.super.loadBlocking( references, deadline );
		}
	}

	@Override
	public void loadBlocking(List<EntityReference> references,
			Map<? super EntityReference, ? super E> entitiesByReference, Deadline deadline) {
		List<? extends E> loadedEntities;
		Long timeout = deadline == null ? null : deadline.remainingTimeMillis();
		try {
			loadedEntities = doLoadEntities( references, timeout );
		}
		catch (QueryTimeoutException | javax.persistence.QueryTimeoutException | LockTimeoutException |
				javax.persistence.LockTimeoutException e) {
			if ( deadline == null ) {
				// ORM-initiated timeout: just propagate the exception.
				throw e;
			}
			throw deadline.forceTimeoutAndCreateException( e );
		}
		Iterator<EntityReference> referencesIterator = references.iterator();
		Iterator<? extends E> loadedEntityIterator = loadedEntities.iterator();
		while ( referencesIterator.hasNext() ) {
			EntityReference reference = referencesIterator.next();
			E loadedEntity = loadedEntityIterator.next();
			if ( loadedEntity != null ) {
				entitiesByReference.put( reference, loadedEntity );
			}
		}
	}

	private List<E> doLoadEntities(List<EntityReference> references, Long timeout) {
		EntityKey[] keys = toEntityKeys( references );
		List<E> loadedEntities = createListContainingNulls( references.size() );

		int fetchSize = loadingOptions.fetchSize();
		Query<? super E> query = createQuery( fetchSize, timeout );

		List<Object> ids = new ArrayList<>( fetchSize );
		for ( int i = 0; i < keys.length; i++ ) {
			EntityKey key = keys[i];
			if ( cacheLookupStrategyImplementor != null ) {
				Object cacheHit = cacheLookupStrategyImplementor.lookup( key );
				if ( cacheHit != null ) {
					EntityReference reference = references.get( i );
					loadedEntities.set( i, castOrNull( reference, cacheHit ) );
					keys[i] = null; // Make sure we won't include this key in the query.
					continue;
				}
			}

			ids.add( key.getIdentifier() );
			if ( ids.size() >= fetchSize ) {
				query.setParameterList( IDS_PARAMETER_NAME, ids );
				// The result is worthless, as entities are not in the right order.
				// However, this will load entities into the persistence context... see further down.
				query.getResultList();
				ids.clear();
			}
		}
		if ( !ids.isEmpty() ) {
			query.setParameterList( IDS_PARAMETER_NAME, ids );
			// Same as above: the result is worthless.
			query.getResultList();
		}

		// All entities are now in the persistence context. Get them!
		for ( int i = 0; i < keys.length; i++ ) {
			EntityKey key = keys[i];
			if ( key == null ) {
				// Already loaded through a cache; skip.
				continue;
			}
			EntityReference reference = references.get( i );
			Object loaded = persistenceContextLookup.lookup( key );
			loadedEntities.set( i, castOrNull( reference, loaded ) );
		}

		return loadedEntities;
	}

	// The cast is safe because we check is an instance of the type from the entity reference.
	@SuppressWarnings("unchecked")
	private E castOrNull(EntityReference reference, Object loadedEntity) {
		if ( !hasExpectedType( reference, loadedEntity ) ) {
			// The index is out of sync and the referenced entity does not exist anymore.
			// Assume the entity we were attempting to load was deleted and mark it as such.
			return null;
		}
		return (E) loadedEntity;
	}

	private Query<? super E> createQuery(int fetchSize, Long timeout) {
		Query<? super E> query = queryFactory.createQueryForLoadByUniqueProperty( session, IDS_PARAMETER_NAME );

		query.setFetchSize( fetchSize );
		if ( timeout != null ) {
			query.setHint( QueryHints.SPEC_HINT_TIMEOUT, Math.toIntExact( timeout ) );
		}

		EntityGraphHint<?> entityGraphHint = loadingOptions.entityGraphHintOrNullForType( entityPersister );
		if ( entityGraphHint != null ) {
			query.applyGraph( entityGraphHint.graph, entityGraphHint.semantic );
		}

		return query;
	}

	private EntityKey[] toEntityKeys(List<EntityReference> references) {
		EntityKey[] entityKeys = new EntityKey[references.size()];
		for ( int i = 0; i < references.size(); i++ ) {
			EntityReference reference = references.get( i );
			EntityKey entityKey = session.generateEntityKey( (Serializable) reference.id(), entityPersister );
			entityKeys[i] = ( entityKey );
		}
		return entityKeys;
	}

	private static <T> List<T> createListContainingNulls(int size) {
		List<T> list = new ArrayList<>( size );
		for ( int i = 0; i < size; i++ ) {
			list.add( null );
		}
		return list;
	}

	/*
	 * Under some circumstances, the multi-access or the cache lookups may return entities that extend E,
	 * but not the type expected by users.
	 *
	 * For example, let's consider entity types A, B, C, D, with B, C, and D extending A
	 * Let's imagine an instance of type B and with id 4 is deleted from the database
	 * and replaced with an instance of type D and id 4.
	 * If a search on entity types B and C is performed before the index is refreshed,
	 * we might be requested to load entity B with id 4,
	 * and since we're working with the common supertype A,
	 * loading will succeed but will yield an entity of type D with id 4.
	 *
	 * Now, the entity will still be an instance of A, but... the user doesn't care about A:
	 * the user asked for a search on entities B and C.
	 * Returning D might be a problem, especially if the user intends to call methods defined on an interface I,
	 * implemented by B and C, but not D.
	 * This will be a problem since that entity does not implement I.
	 *
	 * The easiest way to avoid this problem is to just check the type of every loaded entity,
	 * to be sure it's the same type that was originally requested.
	 * Then we will be safe, because callers are expected to only pass entity references
	 * to types that were originally targeted by the search,
	 * and these types are known to implement any interface that the user could possibly rely on.
	 */
	private static boolean hasExpectedType(EntityReference reference, Object loadedEntity) {
		return reference.type().isInstance( loadedEntity );
	}

}
