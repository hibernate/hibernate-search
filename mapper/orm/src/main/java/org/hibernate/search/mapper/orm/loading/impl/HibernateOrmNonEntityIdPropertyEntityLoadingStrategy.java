/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmEntityLoadingStrategy;
import org.hibernate.search.mapper.orm.loading.spi.LoadingSessionContext;
import org.hibernate.search.mapper.orm.loading.spi.LoadingTypeContext;
import org.hibernate.search.mapper.orm.loading.spi.MutableEntityLoadingOptions;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionEntityLoader;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

public class HibernateOrmNonEntityIdPropertyEntityLoadingStrategy<E, I>
		extends AbstractHibernateOrmLoadingStrategy<E, I> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static <I> HibernateOrmEntityLoadingStrategy<?, ?> create(SessionFactoryImplementor sessionFactory,
			EntityMappingType entityMappingType,
			String documentIdSourcePropertyName, ValueReadHandle<I> documentIdSourceHandle) {
		// By contract, the documentIdSourceHandle and the documentIdSourcePropertyName refer to the same property,
		// whose type is I.
		@SuppressWarnings("unchecked")
		TypeQueryFactory<?, I> queryFactory = (TypeQueryFactory<?, I>) TypeQueryFactory.create( sessionFactory,
				entityMappingType, documentIdSourcePropertyName );
		return new HibernateOrmNonEntityIdPropertyEntityLoadingStrategy<>( sessionFactory,
				entityMappingType, queryFactory,
				documentIdSourcePropertyName, documentIdSourceHandle );
	}

	private final EntityMappingType entityMappingType;
	private final TypeQueryFactory<E, I> queryFactory;
	private final String documentIdSourcePropertyName;
	private final ValueReadHandle<?> documentIdSourceHandle;

	private HibernateOrmNonEntityIdPropertyEntityLoadingStrategy(SessionFactoryImplementor sessionFactory,
			EntityMappingType entityMappingType,
			TypeQueryFactory<E, I> queryFactory,
			String documentIdSourcePropertyName,
			ValueReadHandle<I> documentIdSourceHandle) {
		super( sessionFactory, entityMappingType, queryFactory );
		this.entityMappingType = entityMappingType;
		this.queryFactory = queryFactory;
		this.documentIdSourcePropertyName = documentIdSourcePropertyName;
		this.documentIdSourceHandle = documentIdSourceHandle;
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == null || !( getClass().equals( obj.getClass() ) ) ) {
			return false;
		}
		HibernateOrmNonEntityIdPropertyEntityLoadingStrategy<?, ?> other =
				(HibernateOrmNonEntityIdPropertyEntityLoadingStrategy<?, ?>) obj;
		// If the entity type is different,
		// the factories work in separate ID spaces and should be used separately.
		return entityMappingType.equals( other.entityMappingType )
				&& documentIdSourcePropertyName.equals( other.documentIdSourcePropertyName )
				&& documentIdSourceHandle.equals( other.documentIdSourceHandle );
	}

	@Override
	public int hashCode() {
		return Objects.hash( entityMappingType, documentIdSourcePropertyName, documentIdSourceHandle );
	}

	@Override
	public <E2> PojoSelectionEntityLoader<E2> createLoader(Set<LoadingTypeContext<? extends E2>> targetEntityTypeContexts,
			LoadingSessionContext sessionContext,
			EntityLoadingCacheLookupStrategy cacheLookupStrategy, MutableEntityLoadingOptions loadingOptions) {
		if ( targetEntityTypeContexts.size() != 1 ) {
			throw multipleTypesException( targetEntityTypeContexts );
		}

		return doCreate( targetEntityTypeContexts.iterator().next(), sessionContext, cacheLookupStrategy, loadingOptions );
	}

	private <E2> PojoSelectionEntityLoader<E2> doCreate(LoadingTypeContext<?> targetEntityTypeContext,
			LoadingSessionContext sessionContext,
			EntityLoadingCacheLookupStrategy cacheLookupStrategy,
			MutableEntityLoadingOptions loadingOptions) {
		if ( !entityMappingType.equals( targetEntityTypeContext.entityMappingType() ) ) {
			throw invalidTypeException( targetEntityTypeContext.entityMappingType() );
		}

		/*
		 * We checked just above that "entityMappingType" is equal to "targetEntityTypeContext.entityMappingType()",
		 * so this loader will actually return entities of type E2.
		 */
		@SuppressWarnings("unchecked")
		PojoSelectionEntityLoader<E2> result = new HibernateOrmSelectionEntityByNonIdPropertyLoader<>(
				entityMappingType, (LoadingTypeContext<E2>) targetEntityTypeContext,
				(TypeQueryFactory<E2, ?>) queryFactory,
				documentIdSourcePropertyName, documentIdSourceHandle,
				sessionContext, loadingOptions
		);

		if ( !EntityLoadingCacheLookupStrategy.SKIP.equals( cacheLookupStrategy ) ) {
			/*
			 * We can't support preliminary cache lookup with this strategy,
			 * because document IDs are not entity IDs.
			 * However, we can't throw an exception either,
			 * because this setting may still be relevant for other entity types targeted by the same query.
			 * Let's log something, at least.
			 */
			log.skippingPreliminaryCacheLookupsForNonEntityIdEntityLoader(
					targetEntityTypeContext.jpaEntityName(), cacheLookupStrategy
			);
		}

		return result;
	}

	private AssertionFailure invalidTypeException(EntityMappingType otherEntityMappingType) {
		throw new AssertionFailure(
				"Attempt to use a criteria-based entity loader with an unexpected target entity type."
						+ " Expected entity name: " + entityMappingType.getEntityName()
						+ " Targeted entity name: " + otherEntityMappingType.getEntityName()
		);
	}

	private AssertionFailure multipleTypesException(Set<? extends LoadingTypeContext<?>> targetEntityTypeContexts) {
		return new AssertionFailure(
				"Attempt to use a criteria-based entity loader with multiple target entity types."
						+ " Expected entity name: " + entityMappingType.getEntityName()
						+ " Targeted entity names: "
						+ targetEntityTypeContexts.stream()
								.map( LoadingTypeContext::entityMappingType )
								.map( EntityMappingType::getEntityName )
								.collect( Collectors.toList() )
		);
	}
}
