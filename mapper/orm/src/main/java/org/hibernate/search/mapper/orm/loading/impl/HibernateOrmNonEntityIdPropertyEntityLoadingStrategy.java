/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hibernate.AssertionFailure;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.mapper.orm.search.loading.impl.HibernateOrmComposableSearchEntityLoader;
import org.hibernate.search.mapper.orm.search.loading.impl.MutableEntityLoadingOptions;
import org.hibernate.search.mapper.orm.search.loading.impl.SearchLoadingIndexedTypeContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

public class HibernateOrmNonEntityIdPropertyEntityLoadingStrategy implements EntityLoadingStrategy {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static EntityLoadingStrategy create(SessionFactoryImplementor sessionFactory,
			EntityPersister entityPersister,
			String documentIdSourcePropertyName, ValueReadHandle<?> documentIdSourceHandle) {
		TypeQueryFactory<?> queryFactory = TypeQueryFactory.create( sessionFactory, entityPersister,
				documentIdSourcePropertyName );
		return new HibernateOrmNonEntityIdPropertyEntityLoadingStrategy( entityPersister, queryFactory,
				documentIdSourcePropertyName, documentIdSourceHandle );
	}

	private final EntityPersister entityPersister;
	private final TypeQueryFactory<?> queryFactory;
	private final String documentIdSourcePropertyName;
	private final ValueReadHandle<?> documentIdSourceHandle;

	HibernateOrmNonEntityIdPropertyEntityLoadingStrategy(EntityPersister entityPersister,
			TypeQueryFactory<?> queryFactory,
			String documentIdSourcePropertyName,
			ValueReadHandle<?> documentIdSourceHandle) {
		this.entityPersister = entityPersister;
		this.queryFactory = queryFactory;
		this.documentIdSourcePropertyName = documentIdSourcePropertyName;
		this.documentIdSourceHandle = documentIdSourceHandle;
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == null || !( getClass().equals( obj.getClass() ) ) ) {
			return false;
		}
		HibernateOrmNonEntityIdPropertyEntityLoadingStrategy other = (HibernateOrmNonEntityIdPropertyEntityLoadingStrategy) obj;
		// If the entity type is different,
		// the factories work in separate ID spaces and should be used separately.
		return entityPersister.equals( other.entityPersister )
				&& documentIdSourcePropertyName.equals( other.documentIdSourcePropertyName )
				&& documentIdSourceHandle.equals( other.documentIdSourceHandle );
	}

	@Override
	public int hashCode() {
		return Objects.hash( entityPersister, documentIdSourcePropertyName, documentIdSourceHandle );
	}

	@Override
	public <E> HibernateOrmComposableSearchEntityLoader<E> createLoader(
			SearchLoadingIndexedTypeContext targetEntityTypeContext,
			SessionImplementor session,
			EntityLoadingCacheLookupStrategy cacheLookupStrategy, MutableEntityLoadingOptions loadingOptions) {
		return doCreate( targetEntityTypeContext, session, cacheLookupStrategy, loadingOptions );
	}

	@Override
	public <E> HibernateOrmComposableSearchEntityLoader<? extends E> createLoader(
			List<SearchLoadingIndexedTypeContext> targetEntityTypeContexts,
			SessionImplementor session,
			EntityLoadingCacheLookupStrategy cacheLookupStrategy, MutableEntityLoadingOptions loadingOptions) {
		if ( targetEntityTypeContexts.size() != 1 ) {
			throw new AssertionFailure(
					"Attempt to use a criteria-based entity loader with multiple target entity types."
							+ " There is a bug in Hibernate Search, please report it."
							+ " Expected entity name: " + entityPersister.getEntityName()
							+ " Targeted entity names: "
							+ targetEntityTypeContexts.stream()
							.map( SearchLoadingIndexedTypeContext::hibernateOrmEntityName )
							.collect( Collectors.toList() )
			);
		}

		return doCreate( targetEntityTypeContexts.get( 0 ), session, cacheLookupStrategy, loadingOptions );
	}

	private <E> HibernateOrmComposableSearchEntityLoader<E> doCreate(
			SearchLoadingIndexedTypeContext targetEntityTypeContext,
			SessionImplementor session,
			EntityLoadingCacheLookupStrategy cacheLookupStrategy,
			MutableEntityLoadingOptions loadingOptions) {
		if ( !entityPersister.equals( targetEntityTypeContext.entityPersister() ) ) {
			throw new AssertionFailure(
					"Attempt to use a criteria-based entity loader with an unexpected target entity type."
							+ " There is a bug in Hibernate Search, please report it."
							+ " Expected entity name: " + entityPersister.getEntityName()
							+ " Targeted entity name: " + targetEntityTypeContext.hibernateOrmEntityName()
			);
		}

		/*
		 * We checked just above that "entityPersister" is equal to "targetEntityTypeContext.entityPersister()",
		 * so this loader will actually return entities of type E.
		 */
		@SuppressWarnings("unchecked")
		HibernateOrmComposableSearchEntityLoader<E> result = new HibernateOrmNonEntityIdPropertyEntityLoader<>(
				entityPersister, (TypeQueryFactory<E>) queryFactory,
				documentIdSourcePropertyName, documentIdSourceHandle,
				session, loadingOptions
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
}
