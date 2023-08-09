/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.bootstrap.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Optional;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.search.engine.Version;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.AllAwareConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertyChecker;
import org.hibernate.search.engine.cfg.spi.ConfigurationProvider;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.common.spi.SearchIntegrationEnvironment;
import org.hibernate.search.engine.common.spi.SearchIntegrationPartialBuildState;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.bean.spi.BeanProvider;
import org.hibernate.search.mapper.orm.bootstrap.spi.HibernateOrmIntegrationBooterBehavior;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.cfg.spi.HibernateOrmMapperSpiSettings;
import org.hibernate.search.mapper.orm.common.impl.HibernateOrmUtils;
import org.hibernate.search.mapper.orm.coordination.impl.CoordinationConfigurationContextImpl;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.mapping.impl.HibernateOrmMappingInitiator;
import org.hibernate.search.mapper.orm.mapping.impl.HibernateOrmMappingKey;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.spi.ValueHandleFactory;
import org.hibernate.service.Service;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceContributor;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.jboss.jandex.IndexView;

/**
 * A service that can perform the earliest steps of the integration of Hibernate Search into Hibernate ORM,
 * before {@link HibernateSearchIntegrator} is even called.
 * <p>
 * This is useful in particular when we need to plug in other services into Hibernate ORM,
 * and those services rely on the Hibernate Search configuration.
 */
public abstract class HibernateSearchPreIntegrationService implements Service, AutoCloseable {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ConfigurationProperty<Boolean> ENABLED =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.ENABLED )
					.asBoolean()
					.withDefault( HibernateOrmMapperSettings.Defaults.ENABLED )
					.build();

	private static final ConfigurationProperty<Boolean> LOG_VERSION =
			ConfigurationProperty.forKey( HibernateOrmMapperSpiSettings.JBOSS_LOG_VERSION )
					.asBoolean()
					.withDefault( HibernateOrmMapperSpiSettings.Defaults.JBOSS_LOG_VERSIONS )
					.build();

	public static class Contributor implements ServiceContributor {
		@Override
		public void contribute(StandardServiceRegistryBuilder serviceRegistryBuilder) {
			serviceRegistryBuilder.addInitiator( new Initiator() );
		}
	}

	public static class Initiator implements StandardServiceInitiator<HibernateSearchPreIntegrationService> {
		private boolean initiated = false;

		@Override
		public Class<HibernateSearchPreIntegrationService> getServiceInitiated() {
			return HibernateSearchPreIntegrationService.class;
		}

		@Override
		@SuppressWarnings("rawtypes") // Can't do better: Map is raw in the superclass
		public HibernateSearchPreIntegrationService initiateService(Map configurationValues,
				ServiceRegistryImplementor registry) {
			// Hibernate ORM may call this method twice if we return a null service.
			// Make sure we won't log statements multiple times.
			if ( initiated ) {
				return null;
			}
			initiated = true;

			if ( LOG_VERSION.get( AllAwareConfigurationPropertySource.system() ) ) {
				log.version( Version.versionString() );
			}

			ConfigurationPropertyChecker propertyChecker = ConfigurationPropertyChecker.create();
			@SuppressWarnings("unchecked")
			ConfigurationPropertySource propertySource = propertyChecker.wrap(
					AllAwareConfigurationPropertySource.fromMap(
							HibernateOrmUtils.getServiceOrFail( registry, ConfigurationService.class )
									.getSettings()
					)
			);

			boolean enabled = ENABLED.get( propertySource );
			Optional<HibernateOrmIntegrationPartialBuildState> partialBuildState =
					HibernateOrmIntegrationPartialBuildState.get( propertySource );
			if ( !enabled ) {
				log.debug( "Hibernate Search is disabled through configuration properties." );
				// The partial build state won't get used.
				partialBuildState.ifPresent( HibernateOrmIntegrationPartialBuildState::closeOnFailure );
				// Hibernate Search will not boot.
				return null;
			}

			if ( partialBuildState.isPresent() ) {
				return new PreBooted( propertyChecker, propertySource, partialBuildState.get() );
			}
			else {
				// Most common path (except for Quarkus): Hibernate Search wasn't pre-booted ahead of time,
				// so we will need to perform the first phase of boot now.
				//
				// Do not remove the use of HibernateOrmIntegrationBooterBehavior as an intermediary:
				// its implementation is overridden by Quarkus to make it clear to SubstrateVM
				// that the first phase of boot is never executed in the native binary.
				return HibernateOrmIntegrationBooterBehavior.bootFirstPhase( () -> {
					SearchIntegrationEnvironment environment =
							createEnvironment( propertyChecker, propertySource, registry );
					return new NotBooted( propertyChecker, propertySource, environment, registry );
				} );
			}
		}

		public static SearchIntegrationEnvironment createEnvironment(ConfigurationPropertyChecker propertyChecker,
				ConfigurationPropertySource propertySource, ServiceRegistryImplementor registry) {
			BeanProvider beanProvider = null;
			SearchIntegrationEnvironment.Builder environmentBuilder =
					SearchIntegrationEnvironment.builder( propertySource, propertyChecker );
			try {
				ClassLoaderService hibernateOrmClassLoaderService =
						HibernateOrmUtils.getServiceOrFail( registry, ClassLoaderService.class );
				Optional<ManagedBeanRegistry> managedBeanRegistryService =
						HibernateOrmUtils.getServiceOrEmpty( registry, ManagedBeanRegistry.class );
				HibernateOrmClassLoaderServiceClassAndResourceAndServiceResolver classAndResourceAndServiceResolver =
						new HibernateOrmClassLoaderServiceClassAndResourceAndServiceResolver( hibernateOrmClassLoaderService );

				environmentBuilder.classResolver( classAndResourceAndServiceResolver )
						.resourceResolver( classAndResourceAndServiceResolver )
						.serviceResolver( classAndResourceAndServiceResolver );

				if ( managedBeanRegistryService.isPresent() ) {
					BeanContainer beanContainer = managedBeanRegistryService.get().getBeanContainer();
					if ( beanContainer != null ) {
						// Only use the primary registry, so that we can implement our own fallback when beans are not found
						beanProvider = new HibernateOrmBeanContainerBeanProvider( beanContainer );
						environmentBuilder.beanProvider( beanProvider );
					}
					// else: The given ManagedBeanRegistry only implements fallback: let's ignore it
				}
				return environmentBuilder.build();
			}
			catch (RuntimeException e) {
				new SuppressingCloser( e )
						.push( BeanProvider::close, beanProvider );
				throw e;
			}
		}
	}

	private final ConfigurationPropertyChecker propertyChecker;
	private final ConfigurationPropertySource rawPropertySource;

	private CoordinationConfigurationContextImpl coordinationStrategyConfiguration;

	protected HibernateSearchPreIntegrationService(ConfigurationPropertyChecker propertyChecker,
			ConfigurationPropertySource rawPropertySource) {
		this.propertyChecker = propertyChecker;
		this.rawPropertySource = rawPropertySource;
	}

	@Override
	public final void close() throws Exception {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			doClose( closer );
		}
	}

	protected void doClose(Closer<RuntimeException> closer) {
		closer.push( CoordinationConfigurationContextImpl::close, coordinationStrategyConfiguration );
	}

	/**
	 * @return The raw property source, without a mask or defaults from {@link ConfigurationProvider} applied.
	 * Raw property sources are expected as input to engine SPIs such as
	 * {@link SearchIntegrationEnvironment#builder(ConfigurationPropertySource, ConfigurationPropertyChecker)}
	 * or {@link SearchIntegrationPartialBuildState#finalizer(ConfigurationPropertySource, ConfigurationPropertyChecker)}.
	 * @see SearchIntegrationEnvironment#rootPropertySource(ConfigurationPropertySource, BeanResolver)
	 */
	ConfigurationPropertySource rawPropertySource() {
		return rawPropertySource;
	}

	/**
	 * @return A property source with the proper mask and defaults from {@link ConfigurationProvider} applied.
	 * @see SearchIntegrationEnvironment#rootPropertySource(ConfigurationPropertySource, BeanResolver)
	 */
	abstract ConfigurationPropertySource propertySource();

	public CoordinationConfigurationContextImpl coordinationStrategyConfiguration() {
		if ( coordinationStrategyConfiguration == null ) {
			coordinationStrategyConfiguration =
					CoordinationConfigurationContextImpl.configure( propertySource(), beanResolver() );
		}
		return coordinationStrategyConfiguration;
	}

	ConfigurationPropertyChecker propertyChecker() {
		return propertyChecker;
	}

	abstract BeanResolver beanResolver();

	abstract HibernateOrmIntegrationPartialBuildState doBootFirstPhase(Metadata metadata,
			IndexView jandexIndex, ReflectionManager reflectionManager,
			ValueHandleFactory valueHandleFactory);

	static class NotBooted extends HibernateSearchPreIntegrationService {

		private final SearchIntegrationEnvironment environment;
		private final ServiceRegistry serviceRegistry;

		NotBooted(ConfigurationPropertyChecker propertyChecker,
				ConfigurationPropertySource rawPropertySource,
				SearchIntegrationEnvironment environment,
				ServiceRegistry serviceRegistry) {
			super( propertyChecker, rawPropertySource );
			this.environment = environment;
			this.serviceRegistry = serviceRegistry;
		}

		@Override
		protected void doClose(Closer<RuntimeException> closer) {
			super.doClose( closer );
			closer.push( SearchIntegrationEnvironment::close, environment );
		}

		@Override
		ConfigurationPropertySource propertySource() {
			return environment.propertySource();
		}

		@Override
		BeanResolver beanResolver() {
			return environment.beanResolver();
		}

		@Override
		HibernateOrmIntegrationPartialBuildState doBootFirstPhase(Metadata metadata,
				IndexView jandexIndex, ReflectionManager reflectionManager,
				ValueHandleFactory valueHandleFactory) {
			HibernateOrmMappingInitiator mappingInitiator = null;
			SearchIntegrationPartialBuildState searchIntegrationPartialBuildState = null;
			try {
				SearchIntegration.Builder builder = SearchIntegration.builder( environment );

				HibernateOrmMappingKey mappingKey = new HibernateOrmMappingKey();
				mappingInitiator = HibernateOrmMappingInitiator.create( metadata, jandexIndex, reflectionManager,
						valueHandleFactory, serviceRegistry );
				builder.addMappingInitiator( mappingKey, mappingInitiator );

				searchIntegrationPartialBuildState = builder.prepareBuild();

				return new HibernateOrmIntegrationPartialBuildState(
						searchIntegrationPartialBuildState,
						mappingKey
				);
			}
			catch (RuntimeException e) {
				new SuppressingCloser( e )
						.push( HibernateOrmMappingInitiator::closeOnFailure, mappingInitiator )
						.push( SearchIntegrationPartialBuildState::closeOnFailure, searchIntegrationPartialBuildState );
				throw e;
			}
		}
	}

	static class PreBooted extends HibernateSearchPreIntegrationService {

		private final ConfigurationPropertySource propertySource;
		private final HibernateOrmIntegrationPartialBuildState partialBuildState;

		PreBooted(ConfigurationPropertyChecker propertyChecker,
				ConfigurationPropertySource rawPropertySource,
				HibernateOrmIntegrationPartialBuildState partialBuildState) {
			super( propertyChecker, rawPropertySource );
			this.propertySource = SearchIntegrationEnvironment.rootPropertySource(
					rawPropertySource, partialBuildState.beanResolver() );
			this.partialBuildState = partialBuildState;
		}

		@Override
		protected void doClose(Closer<RuntimeException> closer) {
			super.doClose( closer );
			closer.push( HibernateOrmIntegrationPartialBuildState::closeOnFailure, partialBuildState );
		}

		@Override
		ConfigurationPropertySource propertySource() {
			return propertySource;
		}

		@Override
		BeanResolver beanResolver() {
			return partialBuildState.beanResolver();
		}

		@Override
		HibernateOrmIntegrationPartialBuildState doBootFirstPhase(Metadata metadata, IndexView jandexIndex,
				ReflectionManager reflectionManager, ValueHandleFactory valueHandleFactory) {
			return partialBuildState;
		}
	}
}
