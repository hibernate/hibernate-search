/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.AssertionFailure;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.search.mapper.orm.common.impl.HibernateOrmUtils;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionEntityLoader;

public class HibernateOrmEntityIdEntityLoadingStrategy<E, I>
		extends AbstractHibernateOrmLoadingStrategy<E, I> {

	public static HibernateOrmEntityLoadingStrategy<?, ?> create(SessionFactoryImplementor sessionFactory,
			EntityPersister entityPersister) {
		EntityPersister rootEntityPersister = HibernateOrmUtils.toRootEntityType( sessionFactory, entityPersister );
		TypeQueryFactory<?, ?> queryFactory = TypeQueryFactory.create( sessionFactory, rootEntityPersister,
				entityPersister.getIdentifierPropertyName() );
		return new HibernateOrmEntityIdEntityLoadingStrategy<>( sessionFactory, rootEntityPersister, queryFactory );
	}

	private final EntityPersister rootEntityPersister;
	private final TypeQueryFactory<E, I> queryFactory;

	HibernateOrmEntityIdEntityLoadingStrategy(SessionFactoryImplementor sessionFactory,
			EntityPersister rootEntityPersister, TypeQueryFactory<E, I> queryFactory) {
		super( sessionFactory, rootEntityPersister, queryFactory );
		this.rootEntityPersister = rootEntityPersister;
		this.queryFactory = queryFactory;
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == null || !( getClass().equals( obj.getClass() ) ) ) {
			return false;
		}
		HibernateOrmEntityIdEntityLoadingStrategy<?, ?> other = (HibernateOrmEntityIdEntityLoadingStrategy<?, ?>) obj;
		// If the root entity type is different,
		// the factories work in separate ID spaces and should be used separately.
		return rootEntityPersister.equals( other.rootEntityPersister );
	}

	@Override
	public int hashCode() {
		return rootEntityPersister.hashCode();
	}

	@Override
	public <E2> PojoSelectionEntityLoader<E2> createLoader(Set<LoadingTypeContext<? extends E2>> targetEntityTypeContexts,
			LoadingSessionContext sessionContext, EntityLoadingCacheLookupStrategy cacheLookupStrategy,
			MutableEntityLoadingOptions loadingOptions) {
		if ( targetEntityTypeContexts.size() == 1 ) {
			LoadingTypeContext<? extends E2> targetEntityTypeContext =
					targetEntityTypeContexts.iterator().next();
			/*
			 * This cast is safe: the loader will only return instances of E2.
			 * See PojoLoader.castToExactTypeOrNull() and its callers for more information,
			 * in particular runtime checks handling edge cases.
			 */
			@SuppressWarnings("unchecked")
			PojoSelectionEntityLoader<E2> result = (PojoSelectionEntityLoader<E2>) doCreate( targetEntityTypeContext.entityPersister(), sessionContext,
					cacheLookupStrategy, loadingOptions );
			return result;
		}

		EntityPersister commonSuperType =
				toMostSpecificCommonEntitySuperType( sessionContext.session(), targetEntityTypeContexts );
		if ( commonSuperType == null ) {
			throw invalidTypesException( targetEntityTypeContexts );
		}

		/*
		 * Theoretically, this cast is unsafe,
		 * since the loader could return entities of any type T extending "commonSuperClass",
		 * which is either E2 (good: T = E2)
		 * or a common supertype of some child types of E2
		 * (not good: T might be an interface that E2 doesn't implement but its children do).
		 *
		 * However, we perform some runtime checks that make this cast safe.
		 *
		 * See PojoLoader.castToExactTypeOrNull() and its callers for more information.
		 */
		@SuppressWarnings("unchecked")
		PojoSelectionEntityLoader<E2> result = (PojoSelectionEntityLoader<E2>) doCreate( commonSuperType, sessionContext, cacheLookupStrategy,
				loadingOptions );

		return result;
	}

	@SuppressWarnings("unchecked") // Can't do better here: EntityPersister has no generics
	private PojoSelectionEntityLoader<?> doCreate(EntityPersister entityPersister,
			LoadingSessionContext sessionContext, EntityLoadingCacheLookupStrategy cacheLookupStrategy,
			MutableEntityLoadingOptions loadingOptions) {
		if ( !rootEntityPersister.getMappedClass().isAssignableFrom( entityPersister.getMappedClass() ) ) {
			throw invalidTypeException( entityPersister );
		}

		SessionImplementor session = sessionContext.session();

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
				// We must use the root entity persister here,
				// to avoid a WrongClassException when the type of an entity changes,
				// because that exception cannot be recovered from.
				cacheLookupStrategyImplementor =
						PersistenceContextThenSecondLevelCacheLookupStrategy.create( rootEntityPersister, session );
				break;
			default:
				throw new AssertionFailure( "Unexpected cache lookup strategy: " + cacheLookupStrategy );
		}

		// We must pass rootEntityPersister here, to avoid getting a WrongClassException when loading from the cache,
		// even if we know we actually want instances from the most specific entity persister,
		// because that exception cannot be recovered from.
		return new HibernateOrmSelectionEntityByIdLoader<>( rootEntityPersister, queryFactory, sessionContext,
				persistenceContextLookup, cacheLookupStrategyImplementor, loadingOptions );
	}

	private static EntityPersister toMostSpecificCommonEntitySuperType(SessionImplementor session,
			Iterable<? extends LoadingTypeContext<?>> targetEntityTypeContexts) {
		EntityPersister result = null;
		for ( LoadingTypeContext<?> targetTypeContext : targetEntityTypeContexts ) {
			EntityPersister type = targetTypeContext.entityPersister();
			if ( result == null ) {
				result = type;
			}
			else {
				result = HibernateOrmUtils.toMostSpecificCommonEntitySuperType( result, type );
			}
		}
		return result;
	}

	private AssertionFailure invalidTypeException(EntityPersister otherEntityPersister) {
		throw new AssertionFailure(
				"The targeted entity type is not a subclass of the expected root entity type."
				+ " Expected root entity name: " + rootEntityPersister.getEntityName()
				+ " Targeted entity name: " + otherEntityPersister.getEntityName()
		);
	}

	private AssertionFailure invalidTypesException(
			Set<? extends LoadingTypeContext<?>> targetEntityTypeContexts) {
		return new AssertionFailure(
				"Some types among the targeted entity types are not subclasses of the expected root entity type."
				+ " Expected entity name: " + rootEntityPersister.getEntityName()
				+ " Targeted entity names: "
				+ targetEntityTypeContexts.stream()
						.map( LoadingTypeContext::entityPersister )
						.map( EntityPersister::getEntityName )
						.collect( Collectors.toList() )
		);
	}

}
