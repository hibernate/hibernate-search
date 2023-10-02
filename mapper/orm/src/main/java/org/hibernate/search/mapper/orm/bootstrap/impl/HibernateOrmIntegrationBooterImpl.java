/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.bootstrap.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.beans.container.spi.ExtendedBeanManager;
import org.hibernate.search.mapper.orm.bootstrap.spi.HibernateOrmIntegrationBooter;
import org.hibernate.search.mapper.orm.common.impl.HibernateOrmUtils;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.mapping.impl.HibernateSearchContextProviderService;
import org.hibernate.search.mapper.orm.spi.EnvironmentSynchronizer;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.spi.ValueHandleFactory;
import org.hibernate.service.ServiceRegistry;

import org.jboss.jandex.IndexView;

public class HibernateOrmIntegrationBooterImpl implements HibernateOrmIntegrationBooter {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Metadata metadata;
	private final IndexView jandexIndex;
	private final ReflectionManager reflectionManager;
	private final ValueHandleFactory valueHandleFactory;
	private final HibernateSearchPreIntegrationService preIntegrationService;
	private final Optional<EnvironmentSynchronizer> environmentSynchronizer;

	@SuppressWarnings("deprecation") // There is no alternative to getReflectionManager() at the moment.
	private HibernateOrmIntegrationBooterImpl(BuilderImpl builder) {
		this.metadata = builder.metadata;
		ServiceRegistry serviceRegistry = builder.bootstrapContext.getServiceRegistry();
		this.jandexIndex = builder.bootstrapContext.getJandexView();
		this.reflectionManager = builder.bootstrapContext.getReflectionManager();
		this.valueHandleFactory = builder.valueHandleFactory != null
				? builder.valueHandleFactory
				: ValueHandleFactory.usingMethodHandle( MethodHandles.publicLookup() );
		this.preIntegrationService =
				HibernateOrmUtils.getServiceOrFail( serviceRegistry, HibernateSearchPreIntegrationService.class );

		Optional<EnvironmentSynchronizer> providedEnvironmentSynchronizer =
				HibernateOrmUtils.getServiceOrEmpty( serviceRegistry, EnvironmentSynchronizer.class );
		if ( providedEnvironmentSynchronizer.isPresent() ) {
			// Allow integrators to override the environment synchronizer with an ORM Service
			this.environmentSynchronizer = providedEnvironmentSynchronizer;
		}
		else {
			ConfigurationService ormConfigurationService =
					HibernateOrmUtils.getServiceOrFail( serviceRegistry, ConfigurationService.class );
			Object unknownBeanManager = ormConfigurationService.getSettings().get( AvailableSettings.CDI_BEAN_MANAGER );
			if ( unknownBeanManager == null ) {
				// Try jakarta settings as a default
				// Not getting the constant from AvailableSettings because it does not exist in some ORM versions
				unknownBeanManager = ormConfigurationService.getSettings().get( "jakarta.persistence.bean.manager" );
			}
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

		preIntegrationService.doBootFirstPhase( metadata, jandexIndex, reflectionManager, valueHandleFactory )
				.set( propertyCollector );
	}

	CompletableFuture<HibernateSearchContextProviderService> orchestrateBootAndShutdown(
			CompletionStage<SessionFactoryImplementor> sessionFactoryReadyStage,
			CompletionStage<?> sessionFactoryDestroyingStage) {
		CompletableFuture<HibernateSearchContextProviderService> contextFuture = new CompletableFuture<>();

		CompletableFuture<Void> environmentSynchronizerReadyStage = new CompletableFuture<>();
		CompletableFuture<Void> environmentSynchronizerStartedDestroyingStage = new CompletableFuture<>();

		if ( environmentSynchronizer.isPresent() ) {
			environmentSynchronizer.get().whenEnvironmentDestroying( () -> {
				environmentSynchronizerStartedDestroyingStage.complete( null );
				// If the above triggered shutdown and it failed, the exception will be logged.
			} );
			environmentSynchronizer.get().whenEnvironmentReady( () -> {
				environmentSynchronizerReadyStage.complete( null );
				// If the above triggered bootstrap and it failed, propagate the exception.
				if ( contextFuture.isCompletedExceptionally() ) {
					Futures.unwrappedExceptionJoin( contextFuture );
				}
			} );
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
		bootRequiredStage.thenApply( this::bootNow )
				// Notify whoever wants to hear about the result of the boot.
				.whenComplete( Futures.copyHandler( contextFuture ) );

		/*
		 * As soon as a shutdown is required,
		 * we need to cancel the boot if it's still possible,
		 * or shut down Hibernate Search if it already started.
		 */
		shutdownRequiredStage.thenRun( () -> bootRequiredStage.cancel( false ) );
		// Ignore bootstrap failures
		contextFuture.exceptionally( throwable -> null )
				.thenAcceptBoth( shutdownRequiredStage, (context, ignored) -> {
					if ( context != null ) {
						context.close();
					}
				} )
				// If the above triggered shutdown and it failed, log the exception.
				// We don't propagate it because that may cause the environment
				// to skip further cleanup of other resources.
				.whenComplete( Futures.handler( (ignored, throwable) -> {
					if ( throwable != null ) {
						log.shutdownFailed( throwable );
					}
				} ) );

		return contextFuture;
	}

	private HibernateSearchContextProviderService bootNow(SessionFactoryImplementor sessionFactoryImplementor) {
		HibernateOrmIntegrationPartialBuildState partialBuildState =
				preIntegrationService.doBootFirstPhase( metadata, jandexIndex, reflectionManager, valueHandleFactory );

		try {
			return partialBuildState.doBootSecondPhase( sessionFactoryImplementor,
					preIntegrationService.rawPropertySource(), preIntegrationService.propertyChecker() );
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( HibernateOrmIntegrationPartialBuildState::closeOnFailure, partialBuildState );
			throw e;
		}
	}

	public static class BuilderImpl implements Builder {
		private final Metadata metadata;
		private final BootstrapContext bootstrapContext;

		private ValueHandleFactory valueHandleFactory;

		public BuilderImpl(Metadata metadata, BootstrapContext bootstrapContext) {
			this.metadata = metadata;
			this.bootstrapContext = bootstrapContext;
		}

		@Override
		public Builder valueReadHandleFactory(ValueHandleFactory valueHandleFactory) {
			this.valueHandleFactory = valueHandleFactory;
			return this;
		}

		@Override
		public HibernateOrmIntegrationBooterImpl build() {
			return new HibernateOrmIntegrationBooterImpl( this );
		}
	}
}
