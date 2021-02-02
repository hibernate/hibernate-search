/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.QueryTimeoutException;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.jpa.QueryHints;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.Query;
import org.hibernate.search.engine.common.timing.spi.Deadline;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoader;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

/**
 * An entity loader for indexed entities whose document ID is the entity ID.
 *
 * @param <E> A common supertype of loaded entities.
 */
class HibernateOrmEntityIdEntityLoader<E> implements PojoLoader<E> {

	private static final String IDS_PARAMETER_NAME = "ids";

	private final EntityPersister rootEntityPersister;
	private final TypeQueryFactory<? super E, ?> queryFactory;
	private final LoadingSessionContext sessionContext;
	private final PersistenceContextLookupStrategy persistenceContextLookup;
	private final EntityLoadingCacheLookupStrategyImplementor cacheLookupStrategyImplementor;
	private final MutableEntityLoadingOptions loadingOptions;
	private final boolean singleConcreteTypeInHierarchy;

	HibernateOrmEntityIdEntityLoader(EntityPersister rootEntityPersister,
			TypeQueryFactory<? super E, ?> queryFactory,
			LoadingSessionContext sessionContext,
			PersistenceContextLookupStrategy persistenceContextLookup,
			EntityLoadingCacheLookupStrategyImplementor cacheLookupStrategyImplementor,
			MutableEntityLoadingOptions loadingOptions) {
		this.rootEntityPersister = rootEntityPersister;
		this.queryFactory = queryFactory;
		this.sessionContext = sessionContext;
		this.persistenceContextLookup = persistenceContextLookup;
		this.cacheLookupStrategyImplementor = cacheLookupStrategyImplementor;
		this.loadingOptions = loadingOptions;
		this.singleConcreteTypeInHierarchy =
				rootEntityPersister.getEntityMetamodel().getSubclassEntityNames().size() == 1;
	}

	@Override
	public List<?> loadBlocking(List<?> identifiers, Deadline deadline) {
		Long timeout = deadline == null ? null : deadline.remainingTimeMillis();
		try {
			return doLoadEntities( identifiers, timeout );
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

	@Override
	// The cast is safe because we use reflection to check it.
	@SuppressWarnings("unchecked")
	public <E2 extends E> E2 castToExactTypeOrNull(PojoRawTypeIdentifier<E2> expectedType, Object loadedObject) {
		if ( singleConcreteTypeInHierarchy ) {
			// The loaded object will always be an instance of the exact same type,
			// and we can only get passed that exact type.
			return (E2) loadedObject;
		}
		else if ( expectedType.equals( sessionContext.runtimeIntrospector().detectEntityType( loadedObject ) ) ) {
			return (E2) loadedObject;
		}
		else {
			return null;
		}
	}

	private List<Object> doLoadEntities(List<?> allIds, Long timeout) {
		EntityKey[] keys = toEntityKeys( allIds );
		List<Object> loadedEntities = createListContainingNulls( allIds.size() );

		int fetchSize = loadingOptions.fetchSize();

		List<Object> ids = new ArrayList<>( fetchSize );
		for ( int i = 0; i < keys.length; i++ ) {
			EntityKey key = keys[i];
			if ( cacheLookupStrategyImplementor != null ) {
				Object cacheHit = cacheLookupStrategyImplementor.lookup( key );
				if ( cacheHit != null ) {
					loadedEntities.set( i, cacheHit );
					keys[i] = null; // Make sure we won't include this key in the query.
					continue;
				}
			}

			ids.add( key.getIdentifier() );
			if ( ids.size() >= fetchSize ) {
				// Don't reuse the query; see https://hibernate.atlassian.net/browse/HHH-14439
				Query<? super E> query = createQuery( fetchSize, timeout );
				query.setParameterList( IDS_PARAMETER_NAME, ids );
				// The result is worthless, as entities are not in the right order.
				// However, this will load entities into the persistence context... see further down.
				query.getResultList();
				ids.clear();
			}
		}
		if ( !ids.isEmpty() ) {
			Query<? super E> query = createQuery( fetchSize, timeout );
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
			Object loaded = persistenceContextLookup.lookup( key );
			loadedEntities.set( i, loaded );
		}

		return loadedEntities;
	}

	private Query<? super E> createQuery(int fetchSize, Long timeout) {
		Query<? super E> query = queryFactory.createQueryForLoadByUniqueProperty( sessionContext.session(),
				IDS_PARAMETER_NAME );

		query.setFetchSize( fetchSize );
		if ( timeout != null ) {
			query.setHint( QueryHints.SPEC_HINT_TIMEOUT, Math.toIntExact( timeout ) );
		}

		EntityGraphHint<?> entityGraphHint =
				loadingOptions.entityGraphHintOrNullForType( rootEntityPersister );
		if ( entityGraphHint != null ) {
			query.applyGraph( entityGraphHint.graph, entityGraphHint.semantic );
		}

		return query;
	}

	private EntityKey[] toEntityKeys(List<?> ids) {
		EntityKey[] entityKeys = new EntityKey[ids.size()];
		for ( int i = 0; i < ids.size(); i++ ) {
			Serializable id = (Serializable) ids.get( i );
			// We must use rootEntityPersister here, to avoid getting a WrongClassException when loading from the cache,
			// even if we know we actually want instances from mostSpecificCommonEntityPersister,
			// because that exception can't be ignored: when it occurs, the session is in an undetermined state.
			EntityKey entityKey = sessionContext.session().generateEntityKey( id, rootEntityPersister );
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

}
