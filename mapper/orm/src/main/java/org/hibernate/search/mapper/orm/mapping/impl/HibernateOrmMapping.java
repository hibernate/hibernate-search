/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.common.impl.HibernateOrmUtils;
import org.hibernate.search.mapper.orm.event.impl.HibernateOrmListenerContextProvider;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.mapping.context.HibernateOrmMappingContext;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeIndexedTypeContext;
import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeMappingContext;
import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeSessionContext;
import org.hibernate.search.mapper.orm.scope.impl.SearchScopeImpl;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.mapper.orm.session.AutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.session.impl.ConfiguredAutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSearchSession;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSearchSessionMappingContext;
import org.hibernate.search.mapper.pojo.mapping.spi.AbstractPojoMappingImplementor;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexer;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class HibernateOrmMapping extends AbstractPojoMappingImplementor<HibernateOrmMapping>
		implements SearchMapping, HibernateOrmMappingContext,
				HibernateOrmListenerContextProvider,
				HibernateOrmScopeMappingContext, HibernateOrmSearchSessionMappingContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ConfigurationProperty<BeanReference<? extends AutomaticIndexingSynchronizationStrategy>> AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY )
					.asBeanReference( AutomaticIndexingSynchronizationStrategy.class )
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
			SessionFactoryImplementor sessionFactory, ConfigurationPropertySource propertySource,
			BeanResolver beanResolver) {
		BeanHolder<? extends AutomaticIndexingSynchronizationStrategy> synchronizationStrategyHolder =
				AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY.getAndTransform( propertySource, beanResolver::resolve );

		try {
			log.defaultAutomaticIndexingSynchronizationStrategy( synchronizationStrategyHolder.get() );

			EntityLoadingCacheLookupStrategy cacheLookupStrategy =
					QUERY_LOADING_CACHE_LOOKUP_STRATEGY.get( propertySource );

			int fetchSize = QUERY_LOADING_FETCH_SIZE.get( propertySource );

			return new HibernateOrmMapping(
					mappingDelegate, typeContextContainer, sessionFactory,
					synchronizationStrategyHolder,
					cacheLookupStrategy, fetchSize
			);
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( synchronizationStrategyHolder );
			throw e;
		}
	}

	private final SessionFactoryImplementor sessionFactory;
	private final HibernateOrmTypeContextContainer typeContextContainer;
	private final BeanHolder<? extends AutomaticIndexingSynchronizationStrategy> defaultSynchronizationStrategyHolder;
	private final EntityLoadingCacheLookupStrategy cacheLookupStrategy;
	private final int fetchSize;

	private HibernateOrmMapping(PojoMappingDelegate mappingDelegate,
			HibernateOrmTypeContextContainer typeContextContainer,
			SessionFactoryImplementor sessionFactory,
			BeanHolder<? extends AutomaticIndexingSynchronizationStrategy> defaultSynchronizationStrategyHolder,
			EntityLoadingCacheLookupStrategy cacheLookupStrategy,
			int fetchSize) {
		super( mappingDelegate );
		this.typeContextContainer = typeContextContainer;
		this.sessionFactory = sessionFactory;
		this.defaultSynchronizationStrategyHolder = defaultSynchronizationStrategyHolder;
		this.cacheLookupStrategy = cacheLookupStrategy;
		this.fetchSize = fetchSize;
	}

	@Override
	protected void doClose() {
		defaultSynchronizationStrategyHolder.close();
	}

	@Override
	public <T> SearchScope<T> scope(Collection<? extends Class<? extends T>> types) {
		return createScope( types );
	}

	@Override
	public <T> SearchScope<T> scope(Class<T> expectedSuperType, Collection<String> entityNames) {
		return createScope( expectedSuperType, entityNames );
	}

	@Override
	public EntityManagerFactory toEntityManagerFactory() {
		return sessionFactory;
	}

	@Override
	public SessionFactory toOrmSessionFactory() {
		return sessionFactory;
	}

	@Override
	public IndexManager getIndexManager(String indexName) {
		return getSearchIntegration().getIndexManager( indexName );
	}

	@Override
	public Backend getBackend(String backendName) {
		return getSearchIntegration().getBackend( backendName );
	}

	@Override
	public HibernateOrmMapping toConcreteType() {
		return this;
	}

	@Override
	public PojoIndexer createIndexer(SessionImplementor sessionImplementor, DocumentCommitStrategy commitStrategy) {
		return HibernateOrmSearchSession.get( this, sessionImplementor ).createIndexer( commitStrategy );
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
		return sessionFactory;
	}

	@Override
	public ThreadPoolProvider getThreadPoolProvider() {
		return getDelegate().getThreadPoolProvider();
	}

	@Override
	public FailureHandler getFailureHandler() {
		return getDelegate().getFailureHandler();
	}

	@Override
	public HibernateOrmScopeSessionContext getSessionContext(EntityManager entityManager) {
		return HibernateOrmSearchSession.get( this, HibernateOrmUtils.toSessionImplementor( entityManager ) );
	}

	@Override
	public DetachedBackendSessionContext getDetachedBackendSessionContext(String tenantId) {
		return DetachedBackendSessionContext.of( this, tenantId );
	}

	@Override
	public PojoIndexingPlan getCurrentIndexingPlan(SessionImplementor session, boolean createIfDoesNotExist) {
		return HibernateOrmSearchSession.get( this, session ).getCurrentIndexingPlan( createIfDoesNotExist );
	}

	@Override
	public ConfiguredAutomaticIndexingSynchronizationStrategy getCurrentAutomaticIndexingSynchronizationStrategy(
			SessionImplementor session) {
		return HibernateOrmSearchSession.get( this, session )
				.getConfiguredAutomaticIndexingSynchronizationStrategy();
	}

	@Override
	public HibernateOrmTypeContextContainer getTypeContextProvider() {
		return typeContextContainer;
	}

	@Override
	public <T> SearchScopeImpl<T> createScope(Collection<? extends Class<? extends T>> classes) {
		List<PojoRawTypeIdentifier<? extends T>> typeIdentifiers = new ArrayList<>( classes.size() );
		for ( Class<? extends T> clazz : classes ) {
			typeIdentifiers.add( typeContextContainer.getTypeIdentifierByJavaClass( clazz ) );
		}
		return doCreateScope( typeIdentifiers );
	}

	@Override
	public <T> SearchScopeImpl<T> createScope(Class<T> expectedSuperType, Collection<String> entityNames) {
		List<PojoRawTypeIdentifier<? extends T>> typeIdentifiers = new ArrayList<>( entityNames.size() );
		for ( String entityName : entityNames ) {
			typeIdentifiers.add( getEntityTypeIdentifier( expectedSuperType, entityName ) );
		}
		return doCreateScope( typeIdentifiers );
	}

	@Override
	public HibernateOrmSearchSession.Builder createSessionBuilder(
			SessionImplementor sessionImplementor) {
		SessionFactory givenSessionFactory = sessionImplementor.getSessionFactory();

		if ( !givenSessionFactory.equals( sessionFactory ) ) {
			throw log.usingDifferentSessionFactories( sessionFactory, givenSessionFactory );
		}

		return new HibernateOrmSearchSession.Builder(
				this, typeContextContainer,
				sessionImplementor,
				defaultSynchronizationStrategyHolder.get()
		);
	}

	private SearchIntegration getSearchIntegration() {
		return HibernateSearchContextProviderService.get( getSessionFactory() ).getIntegration();
	}

	private <T> PojoRawTypeIdentifier<? extends T> getEntityTypeIdentifier(Class<T> expectedSuperType,
			String entityName) {
		PojoRawTypeIdentifier<?> typeIdentifier =
				typeContextContainer.getTypeIdentifierByEntityName( entityName );
		Class<?> actualJavaType = typeIdentifier.getJavaClass();
		if ( !expectedSuperType.isAssignableFrom( actualJavaType ) ) {
			throw log.invalidEntitySuperType( entityName, expectedSuperType, actualJavaType );
		}
		// The cast below is safe because we just checked above that the type extends "expectedSuperType", which extends E
		@SuppressWarnings("unchecked")
		PojoRawTypeIdentifier<? extends T> castedTypeIdentifier = (PojoRawTypeIdentifier<? extends T>) typeIdentifier;
		return castedTypeIdentifier;
	}

	private <T> SearchScopeImpl<T> doCreateScope(Collection<PojoRawTypeIdentifier<? extends T>> typeIdentifiers) {
		PojoScopeDelegate<EntityReference, T, HibernateOrmScopeIndexedTypeContext<? extends T>> scopeDelegate =
				getDelegate().createPojoScope(
						this,
						typeIdentifiers,
						typeContextContainer::getIndexedByExactType
				);

		return new SearchScopeImpl<T>( this, scopeDelegate );
	}
}
