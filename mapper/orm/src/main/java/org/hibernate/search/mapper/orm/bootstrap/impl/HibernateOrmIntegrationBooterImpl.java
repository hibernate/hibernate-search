/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.bootstrap.impl;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ExtendedBeanManager;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.common.spi.SearchIntegrationBuilder;
import org.hibernate.search.engine.common.spi.SearchIntegrationPartialBuildState;
import org.hibernate.search.engine.environment.bean.spi.BeanResolver;
import org.hibernate.search.engine.environment.bean.spi.ReflectionBeanResolver;
import org.hibernate.search.mapper.orm.bootstrap.spi.HibernateOrmIntegrationBooter;
import org.hibernate.search.mapper.orm.cfg.impl.ConsumedPropertyKeysReport;
import org.hibernate.search.mapper.orm.cfg.impl.HibernateOrmConfigurationPropertySource;
import org.hibernate.search.mapper.orm.cfg.spi.HibernateOrmMapperSpiSettings;
import org.hibernate.search.mapper.orm.impl.HibernateSearchContextService;
import org.hibernate.search.mapper.orm.mapping.impl.HibernateOrmMappingInitiator;
import org.hibernate.search.mapper.orm.mapping.impl.HibernateOrmMappingKey;
import org.hibernate.search.mapper.orm.mapping.spi.HibernateOrmMapping;
import org.hibernate.search.mapper.orm.spi.EnvironmentSynchronizer;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.service.Service;
import org.hibernate.service.spi.ServiceBinding;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public class HibernateOrmIntegrationBooterImpl implements HibernateOrmIntegrationBooter {

	private static final OptionalConfigurationProperty<HibernateOrmIntegrationPartialBuildState> INTEGRATION_PARTIAL_BUILD_STATE =
			ConfigurationProperty.forKey( HibernateOrmMapperSpiSettings.Radicals.INTEGRATION_PARTIAL_BUILD_STATE )
					.as( HibernateOrmIntegrationPartialBuildState.class, HibernateOrmIntegrationPartialBuildState::parse )
					.build();

	private final Metadata metadata;
	private final ServiceRegistryImplementor serviceRegistry;
	private final ReflectionManager reflectionManager;
	private final Optional<EnvironmentSynchronizer> environmentSynchronizer;
	private final HibernateOrmConfigurationPropertySource propertySource;

	@SuppressWarnings("deprecation") // There is no alternative to getReflectionManager() at the moment.
	public HibernateOrmIntegrationBooterImpl(Metadata metadata, BootstrapContext bootstrapContext) {
		this.metadata = metadata;
		this.serviceRegistry = (ServiceRegistryImplementor) bootstrapContext.getServiceRegistry();
		this.reflectionManager = bootstrapContext.getReflectionManager();
		ConfigurationService configurationService = serviceRegistry.getService( ConfigurationService.class );
		this.propertySource = new HibernateOrmConfigurationPropertySource( configurationService );

		Optional<EnvironmentSynchronizer> providedEnvironmentSynchronizer = getOrmServiceOrEmpty( EnvironmentSynchronizer.class );
		if ( providedEnvironmentSynchronizer.isPresent() ) {
			// Allow integrators to override the environment synchronizer with an ORM Service
			this.environmentSynchronizer = providedEnvironmentSynchronizer;
		}
		else {
			Object unknownBeanManager = configurationService.getSettings().get( AvailableSettings.CDI_BEAN_MANAGER );
			if ( unknownBeanManager instanceof ExtendedBeanManager ) {
				ExtendedBeanManager extendedBeanManager = (ExtendedBeanManager) unknownBeanManager;
				ExtendedBeanManagerSynchronizer synchronizer = new ExtendedBeanManagerSynchronizer();
				extendedBeanManager.registerLifecycleListener( synchronizer );
				this.environmentSynchronizer = Optional.of( synchronizer );
			}
			else {
				this.environmentSynchronizer = Optional.empty();
			}
		}
	}

	@Override
	public void preBoot(BiConsumer<String, Object> propertyCollector) {
		if ( environmentSynchronizer.isPresent() ) {
			throw new AssertionFailure(
					"Cannot pre-boot when an environment synchronizer is used to delay Hibernate Search's bootstrap: "
					+ " we cannot both delay bootstrap and perform it earlier."
			);
		}

		HibernateOrmIntegrationPartialBuildState partialBuildState = doBootFirstPhase();
		propertyCollector.accept( HibernateOrmMapperSpiSettings.INTEGRATION_PARTIAL_BUILD_STATE, partialBuildState );
	}

	CompletableFuture<HibernateSearchContextService> orchestrateBootAndShutdown(
			CompletionStage<SessionFactoryImplementor> sessionFactoryReadyStage,
			CompletionStage<?> sessionFactoryDestroyingStage) {
		CompletableFuture<Void> environmentSynchronizerReadyStage = new CompletableFuture<>();
		CompletableFuture<Void> environmentSynchronizerStartedDestroyingStage = new CompletableFuture<>();

		if ( environmentSynchronizer.isPresent() ) {
			environmentSynchronizer.get().whenEnvironmentDestroying( () -> environmentSynchronizerReadyStage.complete( null ) );
			environmentSynchronizer.get().whenEnvironmentReady( () -> environmentSynchronizerStartedDestroyingStage.complete( null ) );
		}
		else {
			/*
			 * Assume the environment synchronizer is always ready.
			 * Do not care about the "started destroying" event,
			 * if it is not triggered then the session lifecycle will prevail.
			 */
			environmentSynchronizerReadyStage.complete( null );
		}

		/*
		 * Boot is required as soon as both the environment synchronizer *and* the session factory are ready.
		 */
		CompletableFuture<SessionFactoryImplementor> bootRequiredStage =
				environmentSynchronizerReadyStage.thenCombine(
						sessionFactoryReadyStage, (ignored, sessionFactory) -> sessionFactory
				);

		/*
		 * A shutdown is required as soon as the session factory starts being destroyed,
		 * *or* the environment synchronizer signals destroying is starting.
		 */
		CompletionStage<?> shutdownRequiredStage = CompletableFuture.anyOf(
				environmentSynchronizerStartedDestroyingStage, sessionFactoryDestroyingStage.toCompletableFuture()
		);

		/*
		 * As soon as boot is required, we need to, well... boot.
		 */
		CompletableFuture<HibernateSearchContextService> contextBootedFuture =
				bootRequiredStage.thenApply( this::bootNow );

		/*
		 * As soon as a shutdown is required,
		 * we need to cancel the boot if it's still possible,
		 * or shut down Hibernate Search if it already started.
		 */
		shutdownRequiredStage.thenRun( () -> bootRequiredStage.cancel( false ) );
		shutdownRequiredStage.thenAcceptBoth( contextBootedFuture, (ignored, context) -> context.close() );

		return contextBootedFuture;
	}

	private HibernateSearchContextService bootNow(SessionFactoryImplementor sessionFactoryImplementor) {
		Optional<HibernateOrmIntegrationPartialBuildState> partialBuildStateOptional =
				INTEGRATION_PARTIAL_BUILD_STATE.get( propertySource );

		HibernateOrmIntegrationPartialBuildState partialBuildState;
		if ( partialBuildStateOptional.isPresent() ) {
			// The first phase of booting was already performed externally; just re-use the result
			partialBuildState = partialBuildStateOptional.get();
		}
		else {
			partialBuildState = doBootFirstPhase();
		}

		try {
			return doBootSecondPhase( partialBuildState, sessionFactoryImplementor );
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( HibernateOrmIntegrationPartialBuildState::closeOnFailure, partialBuildState );
			throw e;
		}
	}

	/*
	 * Do not change this method's signature and do not stop using it:
	 * it's overridden by Quarkus to make it clear to SubstrateVM
	 * that the first phase of boot is never executed in the native binary.
	 */
	private HibernateOrmIntegrationPartialBuildState doBootFirstPhase() {
		ReflectionBeanResolver reflectionBeanResolver = null;
		BeanResolver beanResolver = null;
		SearchIntegrationPartialBuildState searchIntegrationPartialBuildState = null;
		try {
			propertySource.beforeBoot();

			SearchIntegrationBuilder builder = SearchIntegration.builder( propertySource );

			HibernateOrmMappingKey mappingKey = new HibernateOrmMappingKey();
			HibernateOrmMappingInitiator mappingInitiator = HibernateOrmMappingInitiator.create(
					metadata, reflectionManager, propertySource
			);
			builder.addMappingInitiator( mappingKey, mappingInitiator );

			ClassLoaderService hibernateOrmClassLoaderService = getOrmServiceOrFail( ClassLoaderService.class );
			Optional<ManagedBeanRegistry> managedBeanRegistryService = getOrmServiceOrEmpty( ManagedBeanRegistry.class );
			HibernateOrmClassLoaderServiceClassAndResourceResolver classAndResourceResolver =
					new HibernateOrmClassLoaderServiceClassAndResourceResolver( hibernateOrmClassLoaderService );
			builder.setClassResolver( classAndResourceResolver );
			builder.setResourceResolver( classAndResourceResolver );

			reflectionBeanResolver = new ReflectionBeanResolver( classAndResourceResolver );
			if ( managedBeanRegistryService.isPresent() ) {
				BeanContainer beanContainer = managedBeanRegistryService.get().getBeanContainer();
				if ( beanContainer != null ) {
					// Only use the primary registry, so that we can implement our own fallback when beans are not found
					beanResolver = new HibernateOrmBeanContainerBeanResolver( beanContainer, reflectionBeanResolver );
				}
				// else: The given ManagedBeanRegistry only implements fallback: let's ignore it
			}
			if ( beanResolver == null ) {
				beanResolver = reflectionBeanResolver;
			}
			builder.setBeanResolver( beanResolver );

			// TODO HSEARCH-3057 namingService (JMX)? Or maybe in second phase?

			searchIntegrationPartialBuildState = builder.prepareBuild();

			return new HibernateOrmIntegrationPartialBuildState(
					searchIntegrationPartialBuildState,
					mappingKey,
					propertySource.getConsumedPropertiesReport()
			);
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( SearchIntegrationPartialBuildState::closeOnFailure, searchIntegrationPartialBuildState )
					.push( BeanResolver::close, reflectionBeanResolver )
					.push( BeanResolver::close, beanResolver );
			throw e;
		}
	}

	private HibernateSearchContextService doBootSecondPhase(
			HibernateOrmIntegrationPartialBuildState partialBuildState,
			SessionFactoryImplementor sessionFactoryImplementor) {
		HibernateOrmMapping mapping = partialBuildState.integrationBuildState.finalizeMapping(
				partialBuildState.mappingKey,
				mappingPartialBuildState -> mappingPartialBuildState.bindToSessionFactory( sessionFactoryImplementor, propertySource )
		);
		SearchIntegration integration = partialBuildState.integrationBuildState.finalizeIntegration( propertySource );

		/*
		 * Make the booted integration available to the user (through Search.getFullTextEntityManager(em))
		 * and to the index event listener.
		 */
		HibernateSearchContextService contextService =
				sessionFactoryImplementor.getServiceRegistry().getService( HibernateSearchContextService.class );
		contextService.initialize( integration, mapping );

		// TODO HSEARCH-3057 JMX
//		this.jmx = new JMXHook( propertySource );
//		this.jmx.registerIfEnabled( extendedIntegrator, factory );

		propertySource.afterBoot( partialBuildState.bootFirstPhaseConsumedPropertyKeysReport );

		return contextService;
	}

	private <T extends Service> T getOrmServiceOrFail(Class<T> serviceClass) {
		T service = serviceRegistry.getService( serviceClass );
		if ( service == null ) {
			throw new AssertionFailure(
					"A required service was missing; there is probably a bug in Hibernate ORM or Hibernate Search."
					+ " Missing service: " + serviceClass
			);
		}
		return service;
	}

	private <T extends Service> Optional<T> getOrmServiceOrEmpty(Class<T> serviceClass) {
		/*
		 * First check the service binding, because if it does not exist,
		 * a call to serviceRegistry.getService would throw an exception.
 		 */
		ServiceBinding<T> binding = serviceRegistry.locateServiceBinding( serviceClass );
		if ( binding == null ) {
			// The service binding does not exist, so the service does not exist
			return Optional.empty();
		}
		else {
			// The service binding exists, so the service may exist
			// Retrieve it from the service registry, not from the binding, to be sure it's initialized
			// Note the service may be null, even if the binding is defined
			return Optional.ofNullable( serviceRegistry.getService( serviceClass ) );
		}
	}

	private static final class HibernateOrmIntegrationPartialBuildState {

		static HibernateOrmIntegrationPartialBuildState parse(String stringToParse) {
			throw new AssertionFailure(
					"The partial build state cannot be parsed from a String;"
							+ " it must be null or an instance of " + HibernateOrmIntegrationPartialBuildState.class
			);
		}

		private final SearchIntegrationPartialBuildState integrationBuildState;
		private final HibernateOrmMappingKey mappingKey;
		private final Optional<ConsumedPropertyKeysReport> bootFirstPhaseConsumedPropertyKeysReport;

		HibernateOrmIntegrationPartialBuildState(
				SearchIntegrationPartialBuildState integrationBuildState,
				HibernateOrmMappingKey mappingKey,
				Optional<ConsumedPropertyKeysReport> bootFirstPhaseConsumedPropertyKeysReport) {
			this.integrationBuildState = integrationBuildState;
			this.mappingKey = mappingKey;
			this.bootFirstPhaseConsumedPropertyKeysReport = bootFirstPhaseConsumedPropertyKeysReport;
		}

		void closeOnFailure() {
			this.integrationBuildState.closeOnFailure();
		}
	}
}
