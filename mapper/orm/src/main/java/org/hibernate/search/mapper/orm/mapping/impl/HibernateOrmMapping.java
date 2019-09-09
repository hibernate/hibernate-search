/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import java.lang.invoke.MethodHandles;

import javax.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingSynchronizationStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.event.impl.HibernateOrmListenerContextProvider;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.mapping.context.impl.HibernateOrmMappingContextImpl;
import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeMappingContext;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.mapper.orm.session.AutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSearchSession;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSearchSessionMappingContext;
import org.hibernate.search.mapper.pojo.mapping.spi.AbstractPojoMappingImplementor;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.work.spi.PojoSessionWorkExecutor;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkPlan;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class HibernateOrmMapping extends AbstractPojoMappingImplementor<HibernateOrmMapping>
		implements SearchMapping,
				HibernateOrmListenerContextProvider,
				HibernateOrmScopeMappingContext, HibernateOrmSearchSessionMappingContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ConfigurationProperty<AutomaticIndexingSynchronizationStrategyName> AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY )
					.as( AutomaticIndexingSynchronizationStrategyName.class, AutomaticIndexingSynchronizationStrategyName::of )
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

	public static MappingImplementor<HibernateOrmMapping> create(
			PojoMappingDelegate mappingDelegate, HibernateOrmTypeContextContainer typeContextContainer,
			SessionFactoryImplementor sessionFactory, ConfigurationPropertySource propertySource) {
		AutomaticIndexingSynchronizationStrategyName synchronizationStrategyName =
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

	private final HibernateOrmMappingContextImpl backendMappingContext;
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
		this.backendMappingContext = new HibernateOrmMappingContextImpl( sessionFactory );
		this.synchronizationStrategy = synchronizationStrategy;
		this.cacheLookupStrategy = cacheLookupStrategy;
		this.fetchSize = fetchSize;
	}

	@Override
	public EntityManagerFactory toEntityManagerFactory() {
		return backendMappingContext.getSessionFactory();
	}

	@Override
	public SessionFactory toOrmSessionFactory() {
		return backendMappingContext.getSessionFactory();
	}

	@Override
	public HibernateOrmMapping toConcreteType() {
		return this;
	}

	@Override
	public PojoSessionWorkExecutor createSessionWorkExecutor(SessionImplementor sessionImplementor,
			DocumentCommitStrategy commitStrategy) {
		return HibernateOrmSearchSession.get( this, sessionImplementor ).createSessionWorkExecutor( commitStrategy );
	}

	@Override
	public AutomaticIndexingSynchronizationStrategy getSynchronizationStrategy() {
		return synchronizationStrategy;
	}

	@Override
	public EntityLoadingCacheLookupStrategy getCacheLookupStrategy() {
		return cacheLookupStrategy;
	}

	@Override
	public int getFetchSize() {
		return fetchSize;
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return backendMappingContext.getSessionFactory();
	}

	@Override
	public PojoWorkPlan getCurrentWorkPlan(SessionImplementor session, boolean createIfDoesNotExist) {
		return HibernateOrmSearchSession.get( this, session ).getCurrentWorkPlan( createIfDoesNotExist );
	}

	@Override
	public <E> AbstractHibernateOrmTypeContext<E> getTypeContext(Class<E> type) {
		return typeContextContainer.getByExactClass( type );
	}

	@Override
	public HibernateOrmSearchSession.HibernateOrmSearchSessionBuilder createSessionBuilder(
			SessionImplementor sessionImplementor) {
		SessionFactory expectedSessionFactory = backendMappingContext.getSessionFactory();
		SessionFactory givenSessionFactory = sessionImplementor.getSessionFactory();

		if ( !givenSessionFactory.equals( expectedSessionFactory ) ) {
			throw log.usingDifferentSessionFactories( expectedSessionFactory, givenSessionFactory );
		}

		return new HibernateOrmSearchSession.HibernateOrmSearchSessionBuilder(
				getDelegate(), backendMappingContext, this, typeContextContainer,
				sessionImplementor,
				synchronizationStrategy
		);
	}
}
