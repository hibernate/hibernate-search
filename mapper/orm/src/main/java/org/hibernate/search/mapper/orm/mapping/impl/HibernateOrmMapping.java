/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import java.lang.invoke.MethodHandles;
import javax.persistence.EntityManager;

import org.hibernate.BaseSessionEventListener;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmAutomaticIndexingSynchronizationStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.mapping.context.impl.HibernateOrmMappingContextImpl;
import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeMappingContext;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.mapper.orm.session.AutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSearchSession;
import org.hibernate.search.mapper.pojo.mapping.spi.AbstractPojoMappingImplementor;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.work.spi.PojoSessionWorkExecutor;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.TransientReference;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class HibernateOrmMapping extends AbstractPojoMappingImplementor<HibernateOrmMapping>
		implements HibernateOrmScopeMappingContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ConfigurationProperty<HibernateOrmAutomaticIndexingSynchronizationStrategyName> AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY )
					.as( HibernateOrmAutomaticIndexingSynchronizationStrategyName.class, HibernateOrmAutomaticIndexingSynchronizationStrategyName::of )
					.withDefault( HibernateOrmMapperSettings.Defaults.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY )
					.build();

	private static final ConfigurationProperty<EntityLoadingCacheLookupStrategy> QUERY_LOADING_CACHE_LOOKUP_STRATEGY =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.QUERY_LOADING_CACHE_LOOKUP_STRATEGY )
					.as( EntityLoadingCacheLookupStrategy.class, EntityLoadingCacheLookupStrategy::of )
					.withDefault( HibernateOrmMapperSettings.Defaults.QUERY_LOADING_CACHE_LOOKUP_STRATEGY )
					.build();

	private static final ConfigurationProperty<Integer> QUERY_LOADING_FETCH_SIZE =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.QUERY_LOADING_FETCH_SIZE )
					.asInteger()
					.withDefault( HibernateOrmMapperSettings.Defaults.QUERY_LOADING_FETCH_SIZE )
					.build();

	private static final String SEARCH_SESSION_KEY =
			HibernateOrmMapping.class.getName() + "#SEARCH_SESSION_KEY";

	public static MappingImplementor<HibernateOrmMapping> create(
			PojoMappingDelegate mappingDelegate, HibernateOrmTypeContextContainer typeContextContainer,
			SessionFactoryImplementor sessionFactory, ConfigurationPropertySource propertySource) {
		HibernateOrmAutomaticIndexingSynchronizationStrategyName synchronizationStrategyName =
				AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY.get( propertySource );
		AutomaticIndexingSynchronizationStrategy synchronizationStrategy;
		switch ( synchronizationStrategyName ) {
			case QUEUED:
				synchronizationStrategy = AutomaticIndexingSynchronizationStrategy.queued();
				break;
			case COMMITTED:
				synchronizationStrategy = AutomaticIndexingSynchronizationStrategy.committed();
				break;
			case SEARCHABLE:
				synchronizationStrategy = AutomaticIndexingSynchronizationStrategy.searchable();
				break;
			default:
				throw new AssertionFailure(
						"Unexpected automatic indexing synchronization strategy name: " + synchronizationStrategyName
				);
		}

		EntityLoadingCacheLookupStrategy cacheLookupStrategy =
				QUERY_LOADING_CACHE_LOOKUP_STRATEGY.get( propertySource );

		int fetchSize = QUERY_LOADING_FETCH_SIZE.get( propertySource );

		return new HibernateOrmMapping(
				mappingDelegate, typeContextContainer, sessionFactory,
				synchronizationStrategy,
				cacheLookupStrategy, fetchSize
		);
	}

	private final HibernateOrmMappingContextImpl mappingContext;
	private final HibernateOrmTypeContextContainer typeContextContainer;
	private final AutomaticIndexingSynchronizationStrategy synchronizationStrategy;
	private final EntityLoadingCacheLookupStrategy cacheLookupStrategy;
	private final int fetchSize;

	private HibernateOrmMapping(PojoMappingDelegate mappingDelegate,
			HibernateOrmTypeContextContainer typeContextContainer,
			SessionFactoryImplementor sessionFactory,
			AutomaticIndexingSynchronizationStrategy synchronizationStrategy,
			EntityLoadingCacheLookupStrategy cacheLookupStrategy,
			int fetchSize) {
		super( mappingDelegate );
		this.typeContextContainer = typeContextContainer;
		this.mappingContext = new HibernateOrmMappingContextImpl( sessionFactory );
		this.synchronizationStrategy = synchronizationStrategy;
		this.cacheLookupStrategy = cacheLookupStrategy;
		this.fetchSize = fetchSize;
	}

	@Override
	public HibernateOrmMapping toConcreteType() {
		return this;
	}

	@Override
	public PojoSessionWorkExecutor createSessionWorkExecutor(SessionImplementor sessionImplementor,
			DocumentCommitStrategy commitStrategy) {
		return getSearchSession( sessionImplementor ).createSessionWorkExecutor( commitStrategy );
	}

	@Override
	public EntityLoadingCacheLookupStrategy getCacheLookupStrategy() {
		return cacheLookupStrategy;
	}

	@Override
	public int getFetchSize() {
		return fetchSize;
	}

	/**
	 * @param sessionImplementor A Hibernate session
	 *
	 * @return The {@link HibernateOrmSearchSession} to use within the context of the given session.
	 */
	@SuppressWarnings("unchecked")
	HibernateOrmSearchSession getSearchSession(SessionImplementor sessionImplementor) {
		TransientReference<HibernateOrmSearchSession> reference =
				(TransientReference<HibernateOrmSearchSession>) sessionImplementor.getProperties()
						.get( SEARCH_SESSION_KEY );
		@SuppressWarnings("resource") // The listener below handles closing
		HibernateOrmSearchSession searchSession = reference == null ? null : reference.get();
		if ( searchSession == null ) {
			searchSession = createSessionBuilder( sessionImplementor ).build();
			reference = new TransientReference<>( searchSession );
			sessionImplementor.setProperty( SEARCH_SESSION_KEY, reference );

			// Make sure we will ultimately close the query manager
			sessionImplementor.getEventListenerManager()
					.addListener( new SearchSessionClosingListener( sessionImplementor ) );
		}
		return searchSession;
	}

	<E> AbstractHibernateOrmTypeContext<E> getTypeContext(Class<E> type) {
		return typeContextContainer.getByExactClass( type );
	}

	private HibernateOrmSearchSession.HibernateOrmSearchSessionBuilder createSessionBuilder(EntityManager entityManager) {
		SessionImplementor sessionImplementor;
		try {
			sessionImplementor = entityManager.unwrap( SessionImplementor.class );
		}
		catch (IllegalStateException e) {
			throw log.hibernateSessionAccessError( e );
		}

		SessionFactory expectedSessionFactory = mappingContext.getSessionFactory();
		SessionFactory givenSessionFactory = sessionImplementor.getSessionFactory();

		if ( !givenSessionFactory.equals( expectedSessionFactory ) ) {
			throw log.usingDifferentSessionFactories( expectedSessionFactory, givenSessionFactory );
		}

		return new HibernateOrmSearchSession.HibernateOrmSearchSessionBuilder(
				getDelegate(), mappingContext, this, typeContextContainer,
				sessionImplementor,
				synchronizationStrategy
		);
	}

	private static class SearchSessionClosingListener extends BaseSessionEventListener {
		private final SessionImplementor sessionImplementor;

		private SearchSessionClosingListener(SessionImplementor sessionImplementor) {
			this.sessionImplementor = sessionImplementor;
		}

		@Override
		public void end() {
			@SuppressWarnings("unchecked") // This key "belongs" to us, we know what we put in there.
			TransientReference<HibernateOrmSearchSession> reference =
					(TransientReference<HibernateOrmSearchSession>) sessionImplementor.getProperties()
							.get( SEARCH_SESSION_KEY );
			HibernateOrmSearchSession searchSession = reference == null ? null : reference.get();
			if ( searchSession != null ) {
				searchSession.close();
			}
		}
	}
}
