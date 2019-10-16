/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertyChecker;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.thread.spi.ThreadProvider;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.common.spi.SearchIntegrationFinalizer;
import org.hibernate.search.engine.common.spi.SearchIntegrationPartialBuildState;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.bean.spi.BeanProvider;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingFinalizationContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingFinalizer;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingKey;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingPartialBuildState;
import org.hibernate.search.engine.reporting.impl.RootFailureCollector;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.Futures;

class SearchIntegrationPartialBuildStateImpl implements SearchIntegrationPartialBuildState {

	private static final int FAILURE_LIMIT = 100;

	private final BeanProvider beanProvider;
	private final BeanResolver beanResolver;
	private final BeanHolder<? extends FailureHandler> failureHandlerHolder;
	private final BeanHolder<? extends ThreadProvider> threadProviderHolder;

	private final Map<MappingKey<?, ?>, MappingPartialBuildState> partiallyBuiltMappings;
	private final List<MappingImplementor<?>> fullyBuiltMappings = new ArrayList<>();
	private final Map<String, BackendPartialBuildState> partiallyBuiltBackends;
	private final ConfigurationPropertyChecker partialConfigurationPropertyChecker;

	private final Map<String, BackendImplementor<?>> fullyBuiltBackends = new LinkedHashMap<>();
	private final Map<String, IndexManagerPartialBuildState> partiallyBuiltIndexManagers;
	private final Map<String, IndexManagerImplementor<?>> fullyBuiltIndexManagers = new LinkedHashMap<>();

	SearchIntegrationPartialBuildStateImpl(
			BeanProvider beanProvider, BeanResolver beanResolver,
			BeanHolder<? extends FailureHandler> failureHandlerHolder,
			BeanHolder<? extends ThreadProvider> threadProviderHolder,
			Map<MappingKey<?, ?>, MappingPartialBuildState> partiallyBuiltMappings,
			Map<String, BackendPartialBuildState> partiallyBuiltBackends,
			Map<String, IndexManagerPartialBuildState> partiallyBuiltIndexManagers,
			ConfigurationPropertyChecker partialConfigurationPropertyChecker) {
		this.beanProvider = beanProvider;
		this.beanResolver = beanResolver;
		this.failureHandlerHolder = failureHandlerHolder;
		this.threadProviderHolder = threadProviderHolder;
		this.partiallyBuiltMappings = partiallyBuiltMappings;
		this.partiallyBuiltBackends = partiallyBuiltBackends;
		this.partiallyBuiltIndexManagers = partiallyBuiltIndexManagers;
		this.partialConfigurationPropertyChecker = partialConfigurationPropertyChecker;
	}

	@Override
	public void closeOnFailure() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( MappingPartialBuildState::closeOnFailure, partiallyBuiltMappings.values() );
			closer.pushAll( MappingImplementor::close, fullyBuiltMappings );
			closer.pushAll( IndexManagerPartialBuildState::closeOnFailure, partiallyBuiltIndexManagers.values() );
			closer.pushAll( IndexManagerImplementor::stop, fullyBuiltIndexManagers.values() );
			closer.pushAll( BackendPartialBuildState::closeOnFailure, partiallyBuiltBackends.values() );
			closer.pushAll( BackendImplementor::stop, fullyBuiltBackends.values() );
			closer.pushAll( BeanHolder::close, threadProviderHolder );
			closer.pushAll( BeanHolder::close, failureHandlerHolder );
			closer.pushAll( BeanProvider::close, beanProvider );
		}
	}

	@Override
	public SearchIntegrationFinalizer finalizer(ConfigurationPropertySource propertySource,
			ConfigurationPropertyChecker configurationPropertyChecker) {
		return new SearchIntegrationFinalizerImpl(
				propertySource.withMask( "hibernate.search" ),
				configurationPropertyChecker
		);
	}

	private class SearchIntegrationFinalizerImpl implements SearchIntegrationFinalizer {

		private final ConfigurationPropertySource propertySource;
		private final ConfigurationPropertyChecker propertyChecker;

		private SearchIntegrationFinalizerImpl(ConfigurationPropertySource propertySource,
				ConfigurationPropertyChecker propertyChecker) {
			this.propertySource = propertySource;
			this.propertyChecker = propertyChecker;
		}

		@Override
		public <PBM, M> M finalizeMapping(MappingKey<PBM, M> mappingKey,
				MappingFinalizer<PBM, M> finalizer) {
			// We know this cast will work because of how
			@SuppressWarnings("unchecked")
			PBM partiallyBuiltMapping = (PBM) partiallyBuiltMappings.get( mappingKey );
			if ( partiallyBuiltMapping == null ) {
				throw new AssertionFailure(
						"Some partially built mapping could not be found during bootstrap; there is probably a bug in Hibernate Search."
								+ " Key: " + mappingKey
				);
			}

			MappingFinalizationContext mappingFinalizationContext = new MappingFinalizationContextImpl( propertySource );

			MappingImplementor<M> mapping = finalizer.finalizeMapping( mappingFinalizationContext, partiallyBuiltMapping );
			fullyBuiltMappings.add( mapping );
			partiallyBuiltMappings.remove( mappingKey );

			return mapping.toConcreteType();
		}

		@Override
		public SearchIntegration finalizeIntegration() {
			if ( !partiallyBuiltMappings.isEmpty() ) {
				throw new AssertionFailure(
						"Some mappings were not fully built; there is probably a bug in Hibernate Search."
								+ " Partially built mappings: " + partiallyBuiltMappings
				);
			}

			RootFailureCollector failureCollector = new RootFailureCollector( FAILURE_LIMIT );

			// Start backends
			for ( Map.Entry<String, BackendPartialBuildState> entry : partiallyBuiltBackends.entrySet() ) {
				// TODO HSEARCH-3084 perform backend initialization in parallel for all backends?
				fullyBuiltBackends.put(
						entry.getKey(),
						entry.getValue().finalizeBuild( failureCollector, beanResolver, propertySource )
				);
			}
			failureCollector.checkNoFailure();

			// Start indexes
			CompletableFuture<?>[] indexManagerFutures = new CompletableFuture<?>[partiallyBuiltIndexManagers.size()];
			int indexManagerIndex = 0;
			// Start
			for ( IndexManagerPartialBuildState state : partiallyBuiltIndexManagers.values() ) {
				indexManagerFutures[indexManagerIndex] =
						state.finalizeBuild( failureCollector, beanResolver, propertySource );
				++indexManagerIndex;
			}
			// Wait for the starting operation to finish
			Futures.unwrappedExceptionJoin( CompletableFuture.allOf( indexManagerFutures ) );
			failureCollector.checkNoFailure();
			// Everything went well: register the index managers
			for ( Map.Entry<String, IndexManagerPartialBuildState> entry : partiallyBuiltIndexManagers.entrySet() ) {
				fullyBuiltIndexManagers.put(
						entry.getKey(),
						entry.getValue().getIndexManager()
				);
			}

			propertyChecker.afterBoot( partialConfigurationPropertyChecker, propertySource );

			return new SearchIntegrationImpl(
					beanProvider,
					failureHandlerHolder,
					threadProviderHolder,
					fullyBuiltMappings,
					fullyBuiltBackends,
					fullyBuiltIndexManagers
			);
		}
	}
}
