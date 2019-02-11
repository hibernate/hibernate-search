/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.bootstrap.impl;

import java.util.Optional;
import java.util.function.BiConsumer;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.beans.container.spi.BeanContainer;
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
	private final HibernateOrmConfigurationPropertySource propertySource;

	public HibernateOrmIntegrationBooterImpl(Metadata metadata, ServiceRegistryImplementor serviceRegistry) {
		this.metadata = metadata;
		this.serviceRegistry = serviceRegistry;
		ConfigurationService configurationService = serviceRegistry.getService( ConfigurationService.class );
		this.propertySource = new HibernateOrmConfigurationPropertySource( configurationService );
	}

	@Override
	public void preBoot(BiConsumer<String, Object> propertyCollector) {
		HibernateOrmIntegrationPartialBuildState partialBuildState = doBootFirstPhase();
		propertyCollector.accept( HibernateOrmMapperSpiSettings.INTEGRATION_PARTIAL_BUILD_STATE, partialBuildState );
	}

	private HibernateOrmIntegrationPartialBuildState getOrCreatePartialBuildState() {
		Optional<HibernateOrmIntegrationPartialBuildState> partialBuildStateOptional =
				INTEGRATION_PARTIAL_BUILD_STATE.get( propertySource );

		if ( partialBuildStateOptional.isPresent() ) {
			// The first phase of booting was already performed externally; just re-use the result
			return partialBuildStateOptional.get();
		}
		else {
			return doBootFirstPhase();
		}
	}

	Optional<EnvironmentSynchronizer> getEnvironmentSynchronizer() {
		return getOrmServiceOrEmpty( EnvironmentSynchronizer.class );
	}

	HibernateSearchContextService boot(SessionFactoryImplementor sessionFactoryImplementor) {
		HibernateOrmIntegrationPartialBuildState partialBuildState = getOrCreatePartialBuildState();
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
					metadata, propertySource
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

			// TODO namingService (JMX)? Or maybe in second phase?

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
				mappingPartialBuildState -> mappingPartialBuildState.bindToSessionFactory( sessionFactoryImplementor )
		);
		SearchIntegration integration = partialBuildState.integrationBuildState.finalizeIntegration( propertySource );

		/*
		 * Make the booted integration available to the user (through Search.getFullTextEntityManager(em))
		 * and to the index event listener.
		 */
		HibernateSearchContextService contextService =
				sessionFactoryImplementor.getServiceRegistry().getService( HibernateSearchContextService.class );
		contextService.initialize( integration, mapping );

		// TODO JMX
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
