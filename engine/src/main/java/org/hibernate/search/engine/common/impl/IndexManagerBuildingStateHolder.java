/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaRootNodeBuilder;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;
import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.backend.spi.BackendBuildContext;
import org.hibernate.search.engine.backend.spi.BackendFactory;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.engine.cfg.impl.ConfigurationPropertySourceExtractor;
import org.hibernate.search.engine.cfg.impl.EngineConfigurationUtils;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.mapper.mapping.building.impl.IndexManagerBuildingState;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;


class IndexManagerBuildingStateHolder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final OptionalConfigurationProperty<BeanReference<? extends BackendFactory>> BACKEND_TYPE =
			ConfigurationProperty.forKey( BackendSettings.TYPE ).asBeanReference( BackendFactory.class )
					.build();

	private final BeanResolver beanResolver;
	private final ConfigurationPropertySource propertySource;
	private final RootBuildContext rootBuildContext;

	// Use a LinkedHashMap for deterministic iteration
	private final Map<String, BackendInitialBuildState> backendBuildStateByName = new LinkedHashMap<>();
	// Use a LinkedHashMap for deterministic iteration
	private final Map<String, IndexManagerInitialBuildState> indexManagerBuildStateByName = new LinkedHashMap<>();

	IndexManagerBuildingStateHolder(BeanResolver beanResolver, ConfigurationPropertySource propertySource,
			RootBuildContext rootBuildContext) {
		this.beanResolver = beanResolver;
		this.propertySource = propertySource;
		this.rootBuildContext = rootBuildContext;
	}

	void createBackends(Set<Optional<String>> backendNames) {
		for ( Optional<String> backendNameOptional : backendNames ) {
			String backendName = backendNameOptional.orElse( null );
			EventContext eventContext = EventContexts.fromBackendName( backendName );
			BackendInitialBuildState backendBuildState;
			try {
				backendBuildState = createBackend( backendNameOptional, eventContext );
			}
			catch (RuntimeException e) {
				rootBuildContext.getFailureCollector().withContext( eventContext ).add( e );
				continue;
			}
			backendBuildStateByName.put( backendName, backendBuildState );
		}
	}

	IndexManagerBuildingState getIndexManagerBuildingState(Optional<String> backendName, String indexName,
			String mappedTypeName, boolean multiTenancyEnabled) {
		return getBackend( backendName.orElse( null ) )
				.createIndexManagerBuildingState( indexName, mappedTypeName, multiTenancyEnabled );
	}

	private BackendInitialBuildState getBackend(String backendName) {
		BackendInitialBuildState backendBuildState = backendBuildStateByName.get( backendName );
		if ( backendBuildState == null ) {
			throw new AssertionFailure(
					"Mapper asking for a reference to backend '" + backendName + "', which was not declared in advance."
					+ " There is a bug in Hibernate Search, please report it."
			);
		}
		return backendBuildState;
	}

	Map<String, BackendNonStartedState> getBackendNonStartedStates() {
		// Use a LinkedHashMap for deterministic iteration
		Map<String, BackendNonStartedState> backendsByName = new LinkedHashMap<>();
		for ( Map.Entry<String, BackendInitialBuildState> entry : backendBuildStateByName.entrySet() ) {
			backendsByName.put( entry.getKey(), entry.getValue().getNonStartedState() );
		}
		return backendsByName;
	}

	Map<String, IndexManagerNonStartedState> getIndexManagersNonStartedStates() {
		// Use a LinkedHashMap for deterministic iteration
		Map<String, IndexManagerNonStartedState> indexManagersByName = new LinkedHashMap<>();
		for ( Map.Entry<String, IndexManagerInitialBuildState> entry : indexManagerBuildStateByName.entrySet() ) {
			indexManagersByName.put( entry.getKey(), entry.getValue().getNonStartedState() );
		}
		return indexManagersByName;
	}

	void closeOnFailure(SuppressingCloser closer) {
		closer.pushAll( state -> state.closeOnFailure( closer ), indexManagerBuildStateByName.values() );
		closer.pushAll( BackendInitialBuildState::closeOnFailure, backendBuildStateByName.values() );
	}

	private BackendInitialBuildState createBackend(Optional<String> backendNameOptional, EventContext eventContext) {
		ConfigurationPropertySourceExtractor backendPropertySourceExtractor =
				EngineConfigurationUtils.extractorForBackend( backendNameOptional );
		ConfigurationPropertySource backendPropertySource = backendPropertySourceExtractor.extract( propertySource );
		try ( BeanHolder<? extends BackendFactory> backendFactoryHolder =
				BACKEND_TYPE.<BeanHolder<? extends BackendFactory>>getAndMap( backendPropertySource, beanResolver::resolve )
						.orElseGet( () -> createDefaultBackendFactory( backendPropertySource ) ) ) {
			BackendBuildContext backendBuildContext = new BackendBuildContextImpl( rootBuildContext );

			BackendImplementor backend = backendFactoryHolder.get()
					.create( eventContext, backendBuildContext, backendPropertySource );
			return new BackendInitialBuildState( eventContext, backendPropertySourceExtractor, backendBuildContext,
					backend );
		}
	}

	private BeanHolder<? extends BackendFactory> createDefaultBackendFactory(
			ConfigurationPropertySource backendPropertySource) {
		Map<String, BeanReference<BackendFactory>> referencesByName = beanResolver.namedConfiguredForRole(
				BackendFactory.class );
		if ( referencesByName.isEmpty() ) {
			throw log.noBackendFactoryRegistered( BACKEND_TYPE.resolveOrRaw( backendPropertySource ) );
		}
		else if ( referencesByName.size() > 1 ) {
			throw log.multipleBackendFactoriesRegistered( BACKEND_TYPE.resolveOrRaw( backendPropertySource ),
					referencesByName.keySet() );
		}
		return referencesByName.values().iterator().next().resolve( beanResolver );
	}

	class BackendInitialBuildState {
		private final EventContext eventContext;
		private final ConfigurationPropertySourceExtractor propertySourceExtractor;
		private final BackendBuildContext backendBuildContext;
		private final BackendImplementor backend;

		private BackendInitialBuildState(EventContext eventContext,
				ConfigurationPropertySourceExtractor propertySourceExtractor,
				BackendBuildContext backendBuildContext,
				BackendImplementor backend) {
			this.eventContext = eventContext;
			this.propertySourceExtractor = propertySourceExtractor;
			this.backendBuildContext = backendBuildContext;
			this.backend = backend;
		}

		IndexManagerInitialBuildState createIndexManagerBuildingState(
				String indexName, String mappedTypeName, boolean multiTenancyEnabled) {
			IndexManagerInitialBuildState state = indexManagerBuildStateByName.get( indexName );
			if ( state != null ) {
				throw log.twoTypesTargetSameIndex( indexName, state.mappedTypeName, mappedTypeName );
			}

			ConfigurationPropertySourceExtractor indexPropertySourceExtractor =
					EngineConfigurationUtils.extractorForIndex( propertySourceExtractor, indexName );
			ConfigurationPropertySource indexPropertySource = indexPropertySourceExtractor.extract( propertySource );

			IndexManagerBuilder builder = backend.createIndexManagerBuilder(
					indexName, mappedTypeName, multiTenancyEnabled, backendBuildContext, indexPropertySource
			);
			IndexSchemaRootNodeBuilder schemaRootNodeBuilder = builder.schemaRootNodeBuilder();

			state = new IndexManagerInitialBuildState( indexName, mappedTypeName, indexPropertySourceExtractor,
					builder, schemaRootNodeBuilder );
			indexManagerBuildStateByName.put( indexName, state );
			return state;

		}

		void closeOnFailure() {
			backend.stop();
		}

		BackendNonStartedState getNonStartedState() {
			return new BackendNonStartedState( eventContext, propertySourceExtractor, backend );
		}
	}

	private static class IndexManagerInitialBuildState implements IndexManagerBuildingState {

		private final String indexName;
		private final String mappedTypeName;
		private final ConfigurationPropertySourceExtractor propertySourceExtractor;
		private final IndexManagerBuilder builder;
		private final IndexSchemaRootNodeBuilder schemaRootNodeBuilder;

		private IndexManagerImplementor indexManager;

		IndexManagerInitialBuildState(String indexName, String mappedTypeName,
				ConfigurationPropertySourceExtractor propertySourceExtractor,
				IndexManagerBuilder builder,
				IndexSchemaRootNodeBuilder schemaRootNodeBuilder) {
			this.indexName = indexName;
			this.mappedTypeName = mappedTypeName;
			this.propertySourceExtractor = propertySourceExtractor;
			this.builder = builder;
			this.schemaRootNodeBuilder = schemaRootNodeBuilder;
		}

		void closeOnFailure(SuppressingCloser closer) {
			if ( indexManager != null ) {
				closer.push( IndexManagerImplementor::stop, indexManager );
			}
			else {
				closer.push( IndexManagerBuilder::closeOnFailure, builder );
			}
		}

		@Override
		public String getIndexName() {
			return indexName;
		}

		@Override
		public IndexSchemaRootNodeBuilder getSchemaRootNodeBuilder() {
			return schemaRootNodeBuilder;
		}

		@Override
		public IndexManagerImplementor build() {
			if ( indexManager != null ) {
				throw new AssertionFailure(
						"Trying to build index manager " + indexName + " twice."
						+ " There is probably a bug in the mapper implementation."
				);
			}
			indexManager = builder.build();
			return indexManager;
		}

		IndexManagerNonStartedState getNonStartedState() {
			if ( indexManager == null ) {
				throw new AssertionFailure(
						"Index manager " + indexName + " was not built by the mapper as expected."
						+ " There is probably a bug in the mapper implementation."
				);
			}
			return new IndexManagerNonStartedState( EventContexts.fromIndexName( indexName ),
					propertySourceExtractor, indexManager );
		}
	}

}
