/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.util.List;

import org.hibernate.AssertionFailure;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.search.mapper.orm.common.impl.HibernateOrmUtils;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.mapper.orm.search.loading.impl.HibernateOrmComposableSearchEntityLoader;
import org.hibernate.search.mapper.orm.search.loading.impl.MutableEntityLoadingOptions;
import org.hibernate.search.mapper.orm.search.loading.impl.SearchLoadingIndexedTypeContext;

public class HibernateOrmEntityIdEntityLoadingStrategy implements EntityLoadingStrategy {

	public static EntityLoadingStrategy create(SessionFactoryImplementor sessionFactory,
			EntityPersister entityPersister) {
		return new HibernateOrmEntityIdEntityLoadingStrategy( HibernateOrmUtils.toRootEntityType( sessionFactory, entityPersister ) );
	}

	private final EntityPersister rootEntityPersister;

	HibernateOrmEntityIdEntityLoadingStrategy(EntityPersister rootEntityPersister) {
		this.rootEntityPersister = rootEntityPersister;
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == null || !( getClass().equals( obj.getClass() ) ) ) {
			return false;
		}
		HibernateOrmEntityIdEntityLoadingStrategy other = (HibernateOrmEntityIdEntityLoadingStrategy) obj;
		// If the root entity type is different,
		// the factories work in separate ID spaces and should be used separately.
		return rootEntityPersister.equals( other.rootEntityPersister );
	}

	@Override
	public int hashCode() {
		return rootEntityPersister.hashCode();
	}

	@Override
	public <E> HibernateOrmComposableSearchEntityLoader<E> createLoader(
			SearchLoadingIndexedTypeContext targetEntityTypeContext,
			SessionImplementor session,
			EntityLoadingCacheLookupStrategy cacheLookupStrategy, MutableEntityLoadingOptions loadingOptions) {
		/*
		 * This cast is safe: the loader will only return instances of E.
		 * See hasExpectedType() and its callers for more information,
		 * in particular runtime checks handling edge cases.
		 */
		@SuppressWarnings("unchecked")
		HibernateOrmComposableSearchEntityLoader<E> result = (HibernateOrmComposableSearchEntityLoader<E>) doCreate(
				targetEntityTypeContext.entityPersister(), session, cacheLookupStrategy, loadingOptions
		);
		return result;
	}

	@Override
	public <E> HibernateOrmComposableSearchEntityLoader<? extends E> createLoader(
			List<SearchLoadingIndexedTypeContext> targetEntityTypeContexts,
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
		HibernateOrmComposableSearchEntityLoader<E> result = (HibernateOrmComposableSearchEntityLoader<E>) doCreate(
				commonSuperType, session, cacheLookupStrategy, loadingOptions
		);

		return result;
	}

	private HibernateOrmComposableSearchEntityLoader<?> doCreate(EntityPersister entityPersister,
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
			Iterable<? extends SearchLoadingIndexedTypeContext> targetEntityTypeContexts) {
		MetamodelImplementor metamodel = session.getSessionFactory().getMetamodel();
		EntityPersister result = null;
		for ( SearchLoadingIndexedTypeContext targetTypeContext : targetEntityTypeContexts ) {
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
