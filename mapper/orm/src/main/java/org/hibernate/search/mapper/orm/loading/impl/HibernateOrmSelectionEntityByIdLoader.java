/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.Query;

/**
 * An entity loader for indexed entities whose document ID is the entity ID.
 *
 * @param <E> A common supertype of loaded entities.
 */
class HibernateOrmSelectionEntityByIdLoader<E> extends AbstractHibernateOrmSelectionEntityLoader<E> {

	private final PersistenceContextLookupStrategy persistenceContextLookup;
	private final EntityLoadingCacheLookupStrategyImplementor cacheLookupStrategyImplementor;

	HibernateOrmSelectionEntityByIdLoader(EntityPersister rootEntityPersister,
			TypeQueryFactory<E, ?> queryFactory, LoadingSessionContext sessionContext,
			PersistenceContextLookupStrategy persistenceContextLookup,
			EntityLoadingCacheLookupStrategyImplementor cacheLookupStrategyImplementor,
			MutableEntityLoadingOptions loadingOptions) {
		super( rootEntityPersister, queryFactory, sessionContext, loadingOptions );
		this.persistenceContextLookup = persistenceContextLookup;
		this.cacheLookupStrategyImplementor = cacheLookupStrategyImplementor;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected List<E> doLoadEntities(List<?> allIds, Long timeout) {
		EntityKey[] keys = toEntityKeys( allIds );
		List<E> loadedEntities = createListContainingNulls( allIds.size() );

		int fetchSize = loadingOptions.fetchSize();
		Query<E> query = createQuery( fetchSize, timeout );

		List<Object> ids = new ArrayList<>( fetchSize );
		for ( int i = 0; i < keys.length; i++ ) {
			EntityKey key = keys[i];
			if ( cacheLookupStrategyImplementor != null ) {
				E cacheHit = (E) cacheLookupStrategyImplementor.lookup( key );
				if ( cacheHit != null ) {
					loadedEntities.set( i, cacheHit );
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
			E loaded = (E) persistenceContextLookup.lookup( key );
			loadedEntities.set( i, loaded );
		}

		return loadedEntities;
	}

	private EntityKey[] toEntityKeys(List<?> ids) {
		EntityKey[] entityKeys = new EntityKey[ids.size()];
		for ( int i = 0; i < ids.size(); i++ ) {
			Object id = ids.get( i );
			EntityKey entityKey = sessionContext.session().generateEntityKey( id, entityPersister );
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
