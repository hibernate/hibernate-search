/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.LockModeType;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.common.impl.HibernateOrmUtils;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.loading.EntityIdentifierScroll;
import org.hibernate.search.mapper.pojo.loading.EntityLoader;
import org.hibernate.search.mapper.pojo.massindexing.loader.MassIndexingEntityLoadingStrategy;
import org.hibernate.search.mapper.pojo.massindexing.loader.MassIndexingThreadContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.mapper.pojo.massindexing.loader.MassIndexingEntityLoadingTypeGroup;

abstract class AbstractHibernateOrmLoadingStrategy<E, I> implements
		MassIndexingEntityLoadingStrategy<E, HibernateOrmMassIndexingOptions> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final String ID_PARAMETER_NAME = "ids";

	private final SessionFactoryImplementor sessionFactory;
	private final EntityPersister rootEntityPersister;
	private final TypeQueryFactory<E, I> queryFactory;

	AbstractHibernateOrmLoadingStrategy(
			SessionFactoryImplementor sessionFactory,
			EntityPersister rootEntityPersister, TypeQueryFactory<E, I> queryFactory) {
		this.sessionFactory = sessionFactory;
		this.rootEntityPersister = rootEntityPersister;
		this.queryFactory = queryFactory;
	}

	protected HibernateOrmQueryLoader<E, ?> createQueryLoader(Map<String, ? extends Class<? extends E>> targetEntityTypes) {
		Set<Class<? extends E>> includedTypesFilter;
		if ( HibernateOrmUtils.targetsAllConcreteSubTypes( sessionFactory, rootEntityPersister,
				targetEntityTypes.values() ) ) {
			// All concrete types are included, no need to filter by type.
			includedTypesFilter = Collections.emptySet();
		}
		else {
			includedTypesFilter = new HashSet<>( targetEntityTypes.size() );
			includedTypesFilter.addAll( targetEntityTypes.values() );
		}
		return new HibernateOrmQueryLoader<>( queryFactory, includedTypesFilter );
	}

	@Override
	public EntityIdentifierScroll createIdentifierScroll(HibernateOrmMassIndexingOptions options,
			MassIndexingThreadContext context,
			MassIndexingEntityLoadingTypeGroup<? extends E> loadingTypeGroup) {
		HibernateOrmQueryLoader<E, ?> typeQueryLoader = createQueryLoader( loadingTypeGroup.includedEntityMap() );
		return new HibernateOrmEntityIdentifierScroll<>( typeQueryLoader, options, context );
	}

	@Override
	public EntityLoader<E> createLoader(HibernateOrmMassIndexingOptions options, MassIndexingThreadContext context,
			MassIndexingEntityLoadingTypeGroup<? extends E> loadingTypeGroup) {
		HibernateOrmQueryLoader<E, ?> typeQueryLoader = createQueryLoader( loadingTypeGroup.includedEntityMap() );
		return new HibernateOrmEntityLoader<>( typeQueryLoader, options, context );
	}

	private static class HibernateOrmEntityIdentifierScroll<E, I> implements EntityIdentifierScroll {
		private final MassIndexingThreadContext context;
		private Long totalCount;
		private final ScrollableResults results;
		private final int batchSize;

		public HibernateOrmEntityIdentifierScroll(HibernateOrmQueryLoader<? super E, ?> typeQueryLoader,
				HibernateOrmMassIndexingOptions options,
				MassIndexingThreadContext context) {
			this.context = context;

			int fetchSize = options.idFetchSize();
			batchSize = options.batchSizeToLoadObjects();
			long objectsLimit = options.objectsLimit();

			SharedSessionContractImplementor session = context.context( SharedSessionContractImplementor.class );
			totalCount = typeQueryLoader
					.createCountQuery( session )
					.setCacheable( false ).uniqueResult();

			if ( objectsLimit != 0 && objectsLimit < totalCount ) {
				totalCount = objectsLimit;
			}
			if ( log.isDebugEnabled() ) {
				log.debugf( "going to fetch %d primary keys", totalCount );
			}

			results = typeQueryLoader.createIdentifiersQuery( session )
					.setCacheable( false )
					.setFetchSize( fetchSize ).scroll( ScrollMode.FORWARD_ONLY );

		}

		@Override
		public long totalCount() {
			return totalCount;
		}

		@Override
		public List<I> next() {
			ArrayList<I> destinationList = new ArrayList<>( batchSize );
			long counter = 0;
			SharedSessionContractImplementor sharedSession = context.context( SharedSessionContractImplementor.class );
			while ( results.next() ) {
				@SuppressWarnings("unchecked")
				I id = (I) results.get( 0 );
				destinationList.add( id );
				if ( destinationList.size() == batchSize ) {
					// Explicitly checking whether the TX is still open; Depending on the driver implementation new ids
					// might be produced otherwise if the driver fetches all rows up-front
					if ( !sharedSession.isTransactionInProgress() ) {
						throw log.transactionNotActiveWhileProducingIdsForBatchIndexing();
					}

					return destinationList;
				}
				counter++;
				if ( counter == totalCount ) {
					break;
				}
			}
			return destinationList;
		}

		@Override
		public void close() {
			if ( results != null ) {
				results.close();
			}
		}
	}

	private static class HibernateOrmEntityLoader<E> implements EntityLoader<E> {
		private final HibernateOrmQueryLoader<E, ?> typeQueryLoader;
		private final MassIndexingThreadContext context;
		private final CacheMode cacheMode;

		public HibernateOrmEntityLoader(HibernateOrmQueryLoader<E, ?> typeGroupLoader,
				HibernateOrmMassIndexingOptions options,
				MassIndexingThreadContext context) {
			this.typeQueryLoader = typeGroupLoader;
			this.context = context;
			cacheMode = options.cacheMode();

		}

		@Override
		public List<E> load(List identifiers) {
			SessionImplementor session = context.context( SessionImplementor.class );
			Query<E> query = typeQueryLoader.createLoadingQuery( session, ID_PARAMETER_NAME )
					.setParameter( ID_PARAMETER_NAME, identifiers )
					.setCacheMode( cacheMode )
					.setLockMode( LockModeType.NONE )
					.setCacheable( false )
					.setHibernateFlushMode( FlushMode.MANUAL )
					.setFetchSize( identifiers.size() );
			return query.getResultList();
		}
	}

}
