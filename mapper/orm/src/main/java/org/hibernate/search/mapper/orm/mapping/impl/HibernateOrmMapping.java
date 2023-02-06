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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.reporting.spi.BackendMappingHints;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.engine.mapper.mapping.spi.MappingPreStopContext;
import org.hibernate.search.engine.mapper.mapping.spi.MappingStartContext;
import org.hibernate.search.engine.search.projection.spi.ProjectionMappedTypeContext;
import org.hibernate.search.mapper.orm.automaticindexing.impl.AutomaticIndexingQueueEventProcessingPlanImpl;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingMappingContext;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingQueueEventProcessingPlan;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.common.impl.EntityReferenceImpl;
import org.hibernate.search.mapper.orm.common.impl.HibernateOrmUtils;
import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategy;
import org.hibernate.search.mapper.orm.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.orm.event.impl.HibernateOrmListenerContextProvider;
import org.hibernate.search.mapper.orm.reporting.impl.HibernateOrmMappingHints;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.mapping.context.HibernateOrmMappingContext;
import org.hibernate.search.mapper.orm.mapping.spi.CoordinationStrategyContext;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmRawTypeIdentifierResolver;
import org.hibernate.search.mapper.orm.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.mapper.orm.schema.management.impl.SchemaManagementListener;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeIndexedTypeContext;
import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeMappingContext;
import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeSessionContext;
import org.hibernate.search.mapper.orm.scope.impl.SearchScopeImpl;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.mapper.orm.session.impl.ConfiguredAutomaticIndexingStrategy;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSearchSession;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSearchSessionMappingContext;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSessionTypeContext;
import org.hibernate.search.mapper.orm.spi.BatchMappingContext;
import org.hibernate.search.mapper.orm.tenancy.spi.TenancyConfiguration;
import org.hibernate.search.mapper.pojo.mapping.spi.AbstractPojoMappingImplementor;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexerAgent;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexerAgentCreateContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.work.spi.ConfiguredIndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.schema.management.spi.PojoScopeSchemaManager;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class HibernateOrmMapping extends AbstractPojoMappingImplementor<HibernateOrmMapping>
		implements SearchMapping, AutoCloseable, HibernateOrmMappingContext, EntityReferenceFactory<EntityReference>,
				HibernateOrmListenerContextProvider, BatchMappingContext,
				HibernateOrmScopeMappingContext, HibernateOrmSearchSessionMappingContext,
				AutomaticIndexingMappingContext, CoordinationStrategyContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ConfigurationProperty<EntityLoadingCacheLookupStrategy> QUERY_LOADING_CACHE_LOOKUP_STRATEGY =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.QUERY_LOADING_CACHE_LOOKUP_STRATEGY )
					.as( EntityLoadingCacheLookupStrategy.class, EntityLoadingCacheLookupStrategy::of )
					.withDefault( HibernateOrmMapperSettings.Defaults.QUERY_LOADING_CACHE_LOOKUP_STRATEGY )
					.build();

	private static final ConfigurationProperty<Integer> QUERY_LOADING_FETCH_SIZE =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.QUERY_LOADING_FETCH_SIZE )
					.asIntegerStrictlyPositive()
					.withDefault( HibernateOrmMapperSettings.Defaults.QUERY_LOADING_FETCH_SIZE )
					.build();

	private static final ConfigurationProperty<SchemaManagementStrategyName> SCHEMA_MANAGEMENT_STRATEGY =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.SCHEMA_MANAGEMENT_STRATEGY )
					.as( SchemaManagementStrategyName.class, SchemaManagementStrategyName::of )
					.withDefault( HibernateOrmMapperSettings.Defaults.SCHEMA_MANAGEMENT_STRATEGY )
					.build();

	public static MappingImplementor<HibernateOrmMapping> create(
			PojoMappingDelegate mappingDelegate, HibernateOrmTypeContextContainer typeContextContainer,
			BeanHolder<? extends CoordinationStrategy> coordinationStrategyHolder,
			ConfiguredAutomaticIndexingStrategy configuredAutomaticIndexingStrategy,
			SessionFactoryImplementor sessionFactory, ConfigurationPropertySource propertySource) {
		EntityLoadingCacheLookupStrategy cacheLookupStrategy =
				QUERY_LOADING_CACHE_LOOKUP_STRATEGY.get( propertySource );

		int fetchSize = QUERY_LOADING_FETCH_SIZE.get( propertySource );

		SchemaManagementStrategyName schemaManagementStrategyName = SCHEMA_MANAGEMENT_STRATEGY.get( propertySource );
		SchemaManagementListener schemaManagementListener = new SchemaManagementListener( schemaManagementStrategyName );

		return new HibernateOrmMapping(
				mappingDelegate, typeContextContainer, sessionFactory,
				coordinationStrategyHolder,
				configuredAutomaticIndexingStrategy,
				cacheLookupStrategy, fetchSize,
				schemaManagementListener
		);
	}

	private final SessionFactoryImplementor sessionFactory;
	private final HibernateOrmTypeContextContainer typeContextContainer;
	private final BeanHolder<? extends CoordinationStrategy> coordinationStrategyHolder;
	private final ConfiguredAutomaticIndexingStrategy configuredAutomaticIndexingStrategy;
	private final EntityLoadingCacheLookupStrategy cacheLookupStrategy;
	private final int fetchSize;

	private final SchemaManagementListener schemaManagementListener;

	private TenancyConfiguration tenancyConfiguration;

	private SearchIntegration.Handle integrationHandle;

	private volatile boolean listenerEnabled = true;

	private HibernateOrmMapping(PojoMappingDelegate mappingDelegate,
			HibernateOrmTypeContextContainer typeContextContainer,
			SessionFactoryImplementor sessionFactory,
			BeanHolder<? extends CoordinationStrategy> coordinationStrategyHolder,
			ConfiguredAutomaticIndexingStrategy configuredAutomaticIndexingStrategy,
			EntityLoadingCacheLookupStrategy cacheLookupStrategy,
			int fetchSize,
			SchemaManagementListener schemaManagementListener) {
		super( mappingDelegate );
		this.typeContextContainer = typeContextContainer;
		this.sessionFactory = sessionFactory;
		this.coordinationStrategyHolder = coordinationStrategyHolder;
		this.configuredAutomaticIndexingStrategy = configuredAutomaticIndexingStrategy;
		this.cacheLookupStrategy = cacheLookupStrategy;
		this.fetchSize = fetchSize;
		this.schemaManagementListener = schemaManagementListener;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( SearchIntegration::close, integrationHandle, SearchIntegration.Handle::getOrNull );
			integrationHandle = null;
		}
	}

	@Override
	public CompletableFuture<?> start(MappingStartContext context) {
		integrationHandle = context.integrationHandle();
		// This may fail and normally doesn't involve I/O, so do it first
		configuredAutomaticIndexingStrategy.start(
				this,
				new AutomaticIndexingStrategyStartContextImpl( context ),
				this
		);

		Optional<SearchScopeImpl<Object>> scopeOptional = createAllScope();
		if ( !scopeOptional.isPresent() ) {
			// No indexed type
			return CompletableFuture.completedFuture( null );
		}
		SearchScopeImpl<Object> scope = scopeOptional.get();

		this.tenancyConfiguration =
				TenancyConfiguration.create( delegate().tenancyMode(), context.configurationPropertySource() );

		// Schema management
		PojoScopeSchemaManager schemaManager = scope.schemaManagerDelegate();
		return schemaManagementListener.onStart( context, schemaManager )
				.thenCompose( ignored -> coordinationStrategyHolder.get().start(
						new CoordinationStrategyStartContextImpl( this, context, tenancyConfiguration )
				) );
	}

	@Override
	public CompletableFuture<?> preStop(MappingPreStopContext context) {
		Optional<SearchScopeImpl<Object>> scope = createAllScope();
		if ( !scope.isPresent() ) {
			// No indexed type
			return CompletableFuture.completedFuture( null );
		}
		PojoScopeSchemaManager schemaManager = scope.get().schemaManagerDelegate();
		return coordinationStrategyHolder.get().preStop( new CoordinationStrategyPreStopContextImpl( context ) )
				.thenCompose( ignored -> schemaManagementListener.onStop( context, schemaManager ) );
	}

	@Override
	protected void doStop() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( ConfiguredAutomaticIndexingStrategy::stop, configuredAutomaticIndexingStrategy );
			closer.push( CoordinationStrategy::stop, coordinationStrategyHolder, BeanHolder::get );
			closer.push( BeanHolder::close, coordinationStrategyHolder );
		}
	}

	@Override
	public BackendMappingHints hints() {
		return HibernateOrmMappingHints.INSTANCE;
	}

	@Override
	public ProjectionMappedTypeContext mappedTypeContext(String mappedTypeName) {
		return typeContextContainer.indexedByJpaEntityName().getOrFail( mappedTypeName );
	}

	@Override
	public EntityReferenceFactory<EntityReference> entityReferenceFactory() {
		return this;
	}

	@Override
	public EntityReference createEntityReference(String typeName, Object identifier) {
		HibernateOrmSessionTypeContext<?> typeContext = typeContextContainer.byJpaEntityName().getOrFail( typeName );
		return new EntityReferenceImpl( typeContext.typeIdentifier(), typeContext.jpaEntityName(), identifier );
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
	public <E> SearchIndexedEntity<E> indexedEntity(Class<E> entityType) {
		return typeContextContainer.indexedForExactClass( entityType );
	}

	@Override
	public SearchIndexedEntity<?> indexedEntity(String entityName) {
		return typeContextContainer.indexedByEntityName().getOrFail( entityName );
	}

	@Override
	public Collection<SearchIndexedEntity<?>> allIndexedEntities() {
		return Collections.unmodifiableCollection( typeContextContainer.allIndexed() );
	}

	@Override
	public IndexManager indexManager(String indexName) {
		return searchIntegration().indexManager( indexName );
	}

	@Override
	public Backend backend() {
		return searchIntegration().backend();
	}

	@Override
	public Backend backend(String backendName) {
		return searchIntegration().backend( backendName );
	}

	@Override
	public HibernateOrmMapping toConcreteType() {
		return this;
	}

	@Override
	public EntityLoadingCacheLookupStrategy cacheLookupStrategy() {
		return cacheLookupStrategy;
	}

	@Override
	public int fetchSize() {
		return fetchSize;
	}

	@Override
	public SessionFactoryImplementor sessionFactory() {
		return sessionFactory;
	}

	@Override
	public HibernateOrmScopeSessionContext sessionContext(EntityManager entityManager) {
		return HibernateOrmSearchSession.get( this, HibernateOrmUtils.toSessionImplementor( entityManager ) );
	}

	@Override
	public boolean listenerEnabled() {
		return listenerEnabled;
	}

	// For tests
	public void listenerEnabled(boolean enabled) {
		this.listenerEnabled = enabled;
	}

	// For tests
	public CompletableFuture<?> backgroundIndexingCompletion() {
		return coordinationStrategyHolder.get().completion();
	}

	@Override
	public PojoMassIndexerAgent createMassIndexerAgent(PojoMassIndexerAgentCreateContext context) {
		return coordinationStrategyHolder.get().createMassIndexerAgent( context );
	}

	@Override
	public PojoIndexingPlan currentIndexingPlan(SessionImplementor session,
			boolean createIfDoesNotExist) {
		HibernateOrmSearchSession searchSession = HibernateOrmSearchSession.get( this, session, createIfDoesNotExist );
		if ( searchSession == null ) {
			// Only happens if createIfDoesNotExist is false
			return null;
		}
		return searchSession.currentIndexingPlan( createIfDoesNotExist );
	}

	@Override
	public AutomaticIndexingQueueEventProcessingPlan createIndexingQueueEventProcessingPlan(Session session) {
		HibernateOrmSearchSession searchSession =
				HibernateOrmSearchSession.get( this, session.unwrap( SessionImplementor.class ), true );
		return new AutomaticIndexingQueueEventProcessingPlanImpl( searchSession.createIndexingQueueEventProcessingPlan(),
				entityReferenceFactory() );
	}

	@Override
	public ConfiguredIndexingPlanSynchronizationStrategy<EntityReference> currentAutomaticIndexingSynchronizationStrategy(
			SessionImplementor session) {
		return HibernateOrmSearchSession.get( this, session )
				.configuredAutomaticIndexingSynchronizationStrategy();
	}

	@Override
	public HibernateOrmTypeContextContainer typeContextProvider() {
		return typeContextContainer;
	}

	@Override
	public <T> SearchScopeImpl<T> createScope(Collection<? extends Class<? extends T>> classes) {
		List<PojoRawTypeIdentifier<? extends T>> typeIdentifiers = new ArrayList<>( classes.size() );
		for ( Class<? extends T> clazz : classes ) {
			typeIdentifiers.add( typeContextContainer.typeIdentifierResolver().resolveByJavaClass( clazz ) );
		}
		return doCreateScope( typeIdentifiers );
	}

	@Override
	public <T> SearchScopeImpl<T> createScope(Class<T> expectedSuperType, Collection<String> entityNames) {
		List<PojoRawTypeIdentifier<? extends T>> typeIdentifiers = new ArrayList<>( entityNames.size() );
		for ( String entityName : entityNames ) {
			typeIdentifiers.add( entityTypeIdentifier( expectedSuperType, entityName ) );
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

		return new HibernateOrmSearchSession.Builder( this, typeContextContainer,
				configuredAutomaticIndexingStrategy, sessionImplementor );
	}

	@Override
	public CoordinationStrategy coordinationStrategy() {
		return coordinationStrategyHolder.get();
	}

	private SearchIntegration searchIntegration() {
		return integrationHandle.getOrFail();
	}

	private <T> PojoRawTypeIdentifier<? extends T> entityTypeIdentifier(Class<T> expectedSuperType,
			String entityName) {
		HibernateOrmRawTypeIdentifierResolver resolver = typeContextContainer.typeIdentifierResolver();
		PojoRawTypeIdentifier<?> typeIdentifier = resolver.resolveByJpaOrHibernateOrmEntityName( entityName );
		if ( typeIdentifier == null ) {
			throw log.unknownEntityNameForEntityType( entityName, resolver.allKnownJpaOrHibernateOrmEntityNames() );
		}
		Class<?> actualJavaType = typeIdentifier.javaClass();
		if ( !expectedSuperType.isAssignableFrom( actualJavaType ) ) {
			throw log.invalidEntitySuperType( entityName, expectedSuperType, actualJavaType );
		}
		// The cast below is safe because we just checked above that the type extends "expectedSuperType", which extends E
		@SuppressWarnings("unchecked")
		PojoRawTypeIdentifier<? extends T> castedTypeIdentifier = (PojoRawTypeIdentifier<? extends T>) typeIdentifier;
		return castedTypeIdentifier;
	}

	private Optional<SearchScopeImpl<Object>> createAllScope() {
		return delegate()
				.<EntityReference, HibernateOrmScopeIndexedTypeContext<?>>createPojoAllScope(
						this,
						typeContextContainer::indexedForExactType
				)
				.map( scopeDelegate -> new SearchScopeImpl<>( this, tenancyConfiguration, scopeDelegate ) );
	}

	private <T> SearchScopeImpl<T> doCreateScope(Collection<PojoRawTypeIdentifier<? extends T>> typeIdentifiers) {
		PojoScopeDelegate<EntityReference, T, HibernateOrmScopeIndexedTypeContext<? extends T>> scopeDelegate =
				delegate().createPojoScope(
						this,
						typeIdentifiers,
						typeContextContainer::indexedForExactType
				);

		// Explicit type parameter is necessary here for ECJ (Eclipse compiler)
		return new SearchScopeImpl<T>( this, tenancyConfiguration, scopeDelegate );
	}
}
