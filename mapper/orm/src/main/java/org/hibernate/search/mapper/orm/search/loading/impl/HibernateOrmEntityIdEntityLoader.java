/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.loading.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.common.impl.HibernateOrmUtils;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;

/**
 * An entity loader for indexed entities whose document ID is the entity ID.
 *
 * @param <E> The type of loaded entities.
 */
public class HibernateOrmEntityIdEntityLoader<E> implements HibernateOrmComposableEntityLoader<E> {

	public static EntityLoaderFactory factory(SessionFactoryImplementor sessionFactory,
			EntityPersister entityPersister) {
		return new Factory( HibernateOrmUtils.toRootEntityType( sessionFactory, entityPersister ) );
	}

	private static final String IDS_PARAMETER_NAME = "ids";

	private final SessionImplementor session;
	private final EntityPersister entityPersister;
	private final PersistenceContextLookupStrategy persistenceContextLookup;
	private final EntityLoadingCacheLookupStrategyImplementor cacheLookupStrategyImplementor;
	private final MutableEntityLoadingOptions loadingOptions;

	private HibernateOrmEntityIdEntityLoader(
			EntityPersister entityPersister,
			SessionImplementor session,
			PersistenceContextLookupStrategy persistenceContextLookup,
			EntityLoadingCacheLookupStrategyImplementor cacheLookupStrategyImplementor,
			MutableEntityLoadingOptions loadingOptions) {
		this.entityPersister = entityPersister;
		this.session = session;
		this.persistenceContextLookup = persistenceContextLookup;
		this.cacheLookupStrategyImplementor = cacheLookupStrategyImplementor;
		this.loadingOptions = loadingOptions;
	}

	@Override
	public List<E> loadBlocking(List<EntityReference> references) {
		if ( cacheLookupStrategyImplementor == null ) {
			// Optimization: if we don't need to look up the cache, we don't need a map to store intermediary results.
			return doLoadEntities( references );
		}
		else {
			return HibernateOrmComposableEntityLoader.super.loadBlocking( references );
		}
	}

	@Override
	public void loadBlocking(List<EntityReference> references, Map<? super EntityReference, ? super E> entitiesByReference) {
		List<? extends E> loadedEntities = doLoadEntities( references );
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

	private List<E> doLoadEntities(List<EntityReference> references) {
		EntityKey[] keys = toEntityKeys( references );
		List<E> loadedEntities = createListContainingNulls( references.size() );

		int fetchSize = loadingOptions.fetchSize();
		Query<?> query = createQuery( fetchSize );

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

	private Query<?> createQuery(int fetchSize) {
		Query<?> query = HibernateOrmUtils.createQueryForLoadByUniqueProperty(
				session, entityPersister, entityPersister.getIdentifierPropertyName(), IDS_PARAMETER_NAME
		);

		query.setFetchSize( fetchSize );

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

	private static class Factory implements EntityLoaderFactory {

		private final EntityPersister rootEntityPersister;

		private Factory(EntityPersister rootEntityPersister) {
			this.rootEntityPersister = rootEntityPersister;
		}

		@Override
		public boolean equals(Object obj) {
			if ( obj == null || !( getClass().equals( obj.getClass() ) ) ) {
				return false;
			}
			Factory other = (Factory) obj;
			// If the root entity type is different,
			// the factories work in separate ID spaces and should be used separately.
			return rootEntityPersister.equals( other.rootEntityPersister );
		}

		@Override
		public int hashCode() {
			return rootEntityPersister.hashCode();
		}

		@Override
		public <E> HibernateOrmComposableEntityLoader<E> create(
				HibernateOrmLoadingIndexedTypeContext targetEntityTypeContext,
				SessionImplementor session,
				EntityLoadingCacheLookupStrategy cacheLookupStrategy, MutableEntityLoadingOptions loadingOptions) {
			/*
			 * This cast is safe: the loader will only return instances of E.
			 * See hasExpectedType() and its callers for more information,
			 * in particular runtime checks handling edge cases.
			 */
			@SuppressWarnings("unchecked")
			HibernateOrmComposableEntityLoader<E> result = (HibernateOrmComposableEntityLoader<E>) doCreate(
					targetEntityTypeContext.entityPersister(), session, cacheLookupStrategy, loadingOptions
			);
			return result;
		}

		@Override
		public <E> HibernateOrmComposableEntityLoader<? extends E> create(
				List<HibernateOrmLoadingIndexedTypeContext> targetEntityTypeContexts,
				SessionImplementor session, EntityLoadingCacheLookupStrategy cacheLookupStrategy,
				MutableEntityLoadingOptions loadingOptions) {
			EntityPersister commonSuperType = toMostSpecificCommonEntitySuperType( session, targetEntityTypeContexts );

			/*
			 * Theoretically, this cast is unsafe,
			 * since the loader could return entities of any type T extending "commonSuperClass",
			 * which is either E (good: T = E)
			 * or a common supertype of some child types of E
			 * (not good: T might be an interface that E doesn't implement but its children do).
			 *
			 * However, we perform some runtime checks that make this cast safe.
			 *
			 * See hasExpectedType() and its callers for more information.
			 */
			@SuppressWarnings("unchecked")
			HibernateOrmComposableEntityLoader<E> result = (HibernateOrmComposableEntityLoader<E>) doCreate(
					commonSuperType, session, cacheLookupStrategy, loadingOptions
			);

			return result;
		}

		private HibernateOrmComposableEntityLoader<?> doCreate(EntityPersister entityPersister,
				SessionImplementor session,
				EntityLoadingCacheLookupStrategy cacheLookupStrategy, MutableEntityLoadingOptions loadingOptions) {
			if ( !rootEntityPersister.getMappedClass().isAssignableFrom( entityPersister.getMappedClass() ) ) {
				throw new AssertionFailure(
						"Some types among the targeted entity types are not subclasses of the expected root entity type."
								+ " There is a bug in Hibernate Search, please report it."
								+ " Expected root entity name: " + rootEntityPersister.getEntityName()
								+ " Targeted entity name: " + entityPersister.getEntityName()
				);
			}

			PersistenceContextLookupStrategy persistenceContextLookup =
					PersistenceContextLookupStrategy.create( session );
			EntityLoadingCacheLookupStrategyImplementor cacheLookupStrategyImplementor;

			/*
			 * Ideally, in order to comply with the cache lookup strategy,
			 * we would use multiAccess setters such as
			 * with(CacheMode) and enableSessionCheck(boolean),
			 * and let Hibernate ORM do it for us.
			 *
			 * However, with(CacheMode) has a side-effect: it can also affect how entities are put into the cache.
			 * Since the cache lookup strategy has nothing to do with that,
			 * we go the safer route and wrap the loader with other loaders that
			 * will perform PC and 2LC checking prior to using the multiAccess.
			 */
			switch ( cacheLookupStrategy ) {
				case SKIP:
					cacheLookupStrategyImplementor = null;
					break;
				case PERSISTENCE_CONTEXT:
					cacheLookupStrategyImplementor = persistenceContextLookup;
					break;
				case PERSISTENCE_CONTEXT_THEN_SECOND_LEVEL_CACHE:
					cacheLookupStrategyImplementor =
							PersistenceContextThenSecondLevelCacheLookupStrategy.create( entityPersister, session );
					break;
				default:
					throw new AssertionFailure( "Unexpected cache lookup strategy: " + cacheLookupStrategy );
			}

			return new HibernateOrmEntityIdEntityLoader<>(
					entityPersister, session, persistenceContextLookup, cacheLookupStrategyImplementor, loadingOptions
			);
		}

		private static EntityPersister toMostSpecificCommonEntitySuperType(SessionImplementor session,
				Iterable<? extends HibernateOrmLoadingIndexedTypeContext> targetEntityTypeContexts) {
			MetamodelImplementor metamodel = session.getSessionFactory().getMetamodel();
			EntityPersister result = null;
			for ( HibernateOrmLoadingIndexedTypeContext targetTypeContext : targetEntityTypeContexts ) {
				EntityPersister type = targetTypeContext.entityPersister();
				if ( result == null ) {
					result = type;
				}
				else {
					result = HibernateOrmUtils.toMostSpecificCommonEntitySuperType( metamodel, result, type );
				}
			}
			return result;
		}

	}
}
