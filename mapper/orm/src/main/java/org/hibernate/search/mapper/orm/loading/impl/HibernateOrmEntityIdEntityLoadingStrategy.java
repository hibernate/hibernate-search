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
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.search.mapper.orm.common.impl.HibernateOrmUtils;
import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmEntityLoadingStrategy;
import org.hibernate.search.mapper.orm.loading.spi.LoadingSessionContext;
import org.hibernate.search.mapper.orm.loading.spi.MutableEntityLoadingOptions;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingTypeContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionEntityLoader;

public class HibernateOrmEntityIdEntityLoadingStrategy<E, I>
		extends AbstractHibernateOrmLoadingStrategy<E, I> {

	public static HibernateOrmEntityLoadingStrategy<?, ?> create(PersistentClass persistentClass) {
		var rootClass = persistentClass.getRootClass();
		return create( rootClass, HibernateOrmUtils.entityClass( rootClass ) );
	}

	private static <E> HibernateOrmEntityIdEntityLoadingStrategy<E, ?> create(RootClass rootClass, Class<E> rootMappedClass) {
		var idProperty = rootClass.getIdentifierProperty();
		TypeQueryFactory<E, ?> queryFactory = TypeQueryFactory.create( rootMappedClass, rootClass.getEntityName(),
				idProperty.getType().getReturnedClass(), idProperty.getName(),
				true );
		return new HibernateOrmEntityIdEntityLoadingStrategy<>( rootMappedClass, rootClass.getEntityName(),
				queryFactory );
	}

	private final Class<E> rootEntityClass;
	private final TypeQueryFactory<E, I> queryFactory;

	HibernateOrmEntityIdEntityLoadingStrategy(Class<E> rootEntityClass, String rootEntityName,
			TypeQueryFactory<E, I> queryFactory) {
		super( rootEntityName, queryFactory );
		this.rootEntityClass = rootEntityClass;
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
		return rootEntityName.equals( other.rootEntityName );
	}

	@Override
	public int hashCode() {
		return rootEntityClass.hashCode();
	}

	@Override
	public PojoSelectionEntityLoader<E> createEntityLoader(
			Set<? extends PojoLoadingTypeContext<? extends E>> targetEntityTypeContexts,
			HibernateOrmSelectionLoadingContext loadingContext) {
		var sessionFactory = loadingContext.sessionImplementor().getSessionFactory();
		EntityMappingType commonSuperType = toMostSpecificCommonEntitySuperType( sessionFactory, targetEntityTypeContexts );
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
		PojoSelectionEntityLoader<E> result =
				(PojoSelectionEntityLoader<E>) doCreate( commonSuperType, loadingContext.sessionContext(),
						loadingContext.cacheLookupStrategy(), loadingContext.loadingOptions() );

		return result;
	}

	private PojoSelectionEntityLoader<?> doCreate(EntityMappingType entityMappingType,
			LoadingSessionContext sessionContext, EntityLoadingCacheLookupStrategy cacheLookupStrategy,
			MutableEntityLoadingOptions loadingOptions) {
		var session = sessionContext.session();
		var sessionFactory = session.getSessionFactory();
		EntityMappingType rootEntityMappingType = HibernateOrmUtils.entityMappingType( sessionFactory, rootEntityName );
		if ( !rootEntityClass.isAssignableFrom( entityMappingType.getJavaType().getJavaTypeClass() ) ) {
			throw invalidTypeException( entityMappingType );
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
				// We must use the root entity persister here,
				// to avoid a WrongClassException when the type of an entity changes,
				// because that exception cannot be recovered from.
				cacheLookupStrategyImplementor =
						PersistenceContextThenSecondLevelCacheLookupStrategy.create( rootEntityMappingType, session );
				break;
			default:
				throw new AssertionFailure( "Unexpected cache lookup strategy: " + cacheLookupStrategy );
		}

		// We must pass rootEntityMappingType here, to avoid getting a WrongClassException when loading from the cache,
		// even if we know we actually want instances from the most specific entity type,
		// because that exception cannot be recovered from.
		return new HibernateOrmSelectionEntityByIdLoader<>( rootEntityMappingType, queryFactory, sessionContext,
				persistenceContextLookup, cacheLookupStrategyImplementor, loadingOptions );
	}

	private static EntityMappingType toMostSpecificCommonEntitySuperType(
			SessionFactoryImplementor sessionFactory,
			Iterable<? extends PojoLoadingTypeContext<?>> targetEntityTypeContexts) {
		EntityMappingType result = null;
		for ( PojoLoadingTypeContext<?> targetTypeContext : targetEntityTypeContexts ) {
			EntityMappingType type = HibernateOrmUtils.entityMappingType( sessionFactory,
					targetTypeContext.secondaryEntityName() );
			if ( result == null ) {
				result = type;
			}
			else {
				result = HibernateOrmUtils.toMostSpecificCommonEntitySuperType( result, type );
			}
		}
		return result;
	}

	private AssertionFailure invalidTypeException(EntityMappingType otherEntityMappingType) {
		throw new AssertionFailure(
				"The targeted entity type is not a subclass of the expected root entity type."
						+ " Expected root entity name: " + rootEntityName
						+ " Targeted entity name: " + otherEntityMappingType.getEntityName()
		);
	}

	private AssertionFailure invalidTypesException(
			Set<? extends PojoLoadingTypeContext<?>> targetEntityTypeContexts) {
		return new AssertionFailure(
				"Some types among the targeted entity types are not subclasses of the expected root entity type."
						+ " Expected entity name: " + rootEntityName
						+ " Targeted entity names: "
						+ targetEntityTypeContexts.stream()
								.map( PojoLoadingTypeContext::secondaryEntityName )
								.collect( Collectors.toUnmodifiableList() )
		);
	}

}
