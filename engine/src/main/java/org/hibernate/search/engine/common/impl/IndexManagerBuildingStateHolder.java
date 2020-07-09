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
import org.hibernate.search.engine.cfg.EngineSettings;
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



class IndexManagerBuildingStateHolder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ConfigurationProperty<String> EXPLICIT_DEFAULT_BACKEND_NAME =
			ConfigurationProperty.forKey( EngineSettings.Radicals.DEFAULT_BACKEND ).asString()
					.withDefault( "default" ).build();

	private static final OptionalConfigurationProperty<BeanReference<? extends BackendFactory>> BACKEND_TYPE =
			ConfigurationProperty.forKey( BackendSettings.TYPE ).asBeanReference( BackendFactory.class )
					.build();

	private final BeanResolver beanResolver;
	private final ConfigurationPropertySource propertySource;
	private final RootBuildContext rootBuildContext;

	private final String defaultBackendName;

	// Use a LinkedHashMap for deterministic iteration
	private final Map<String, BackendInitialBuildState> backendBuildStateByName = new LinkedHashMap<>();
	// Use a LinkedHashMap for deterministic iteration
	private final Map<String, IndexManagerInitialBuildState> indexManagerBuildStateByName = new LinkedHashMap<>();

	IndexManagerBuildingStateHolder(BeanResolver beanResolver, ConfigurationPropertySource propertySource,
			RootBuildContext rootBuildContext) {
		this.beanResolver = beanResolver;
		this.propertySource = propertySource;
		this.rootBuildContext = rootBuildContext;
		defaultBackendName = EXPLICIT_DEFAULT_BACKEND_NAME.get( propertySource );
	}

	void createBackends(Set<Optional<String>> backendNames) {
		if ( backendNames.remove( Optional.<String>empty() ) ) {
			backendNames.add( Optional.of( defaultBackendName ) );
		}
		for ( Optional<String> backendNameOptional : backendNames ) {
			String backendName = backendNameOptional.get(); // Never empty, see above
			BackendInitialBuildState backendBuildState;
			try {
				backendBuildState = createBackend( backendName );
			}
			catch (RuntimeException e) {
				rootBuildContext.getFailureCollector()
						.withContext( EventContexts.fromBackendName( backendName ) )
						.add( e );
				continue;
			}
			backendBuildStateByName.put( backendName, backendBuildState );
		}
	}

	IndexManagerBuildingState getIndexManagerBuildingState(Optional<String> backendName, String indexName,
			String mappedTypeName, boolean multiTenancyEnabled) {
		return getBackend( backendName.orElse( defaultBackendName ) )
				.getIndexManagerBuildingState( indexName, mappedTypeName, multiTenancyEnabled );
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

	private BackendInitialBuildState createBackend(String backendName) {
		ConfigurationPropertySource backendPropertySource =
				EngineConfigurationUtils.getBackend( propertySource, backendName );
		try ( BeanHolder<? extends BackendFactory> backendFactoryHolder =
				BACKEND_TYPE.getAndMapOrThrow(
						backendPropertySource,
						beanResolver::resolve,
						key -> log.backendTypeCannotBeNullOrEmpty( backendName, key )
				) ) {
			BackendBuildContext backendBuildContext = new BackendBuildContextImpl( rootBuildContext );

			BackendImplementor backend = backendFactoryHolder.get()
					.create( backendName, backendBuildContext, backendPropertySource );
			return new BackendInitialBuildState( backendName, backendBuildContext, backendPropertySource, backend );
		}
	}

	class BackendInitialBuildState {
		private final String backendName;
		private final BackendBuildContext backendBuildContext;
		private final ConfigurationPropertySource backendPropertySource;
		private final ConfigurationPropertySource defaultIndexPropertySource;
		private final BackendImplementor backend;

		private BackendInitialBuildState(
				String backendName,
				BackendBuildContext backendBuildContext,
				ConfigurationPropertySource backendPropertySource,
				BackendImplementor backend) {
			this.backendName = backendName;
			this.backendBuildContext = backendBuildContext;
			this.backendPropertySource = backendPropertySource;
			this.defaultIndexPropertySource =
					EngineConfigurationUtils.getIndexDefaults( backendPropertySource );
			this.backend = backend;
		}

		IndexManagerInitialBuildState getIndexManagerBuildingState(
				String indexName, String mappedTypeName, boolean multiTenancyEnabled) {
			IndexManagerInitialBuildState state = indexManagerBuildStateByName.get( indexName );
			if ( state == null ) {
				ConfigurationPropertySource indexPropertySource =
						EngineConfigurationUtils.getIndex( backendPropertySource, defaultIndexPropertySource, indexName );

				IndexManagerBuilder builder = backend.createIndexManagerBuilder(
						indexName, mappedTypeName, multiTenancyEnabled, backendBuildContext, indexPropertySource
				);
				IndexSchemaRootNodeBuilder schemaRootNodeBuilder = builder.schemaRootNodeBuilder();

				state = new IndexManagerInitialBuildState( backendName, indexName, builder, schemaRootNodeBuilder );
				indexManagerBuildStateByName.put( indexName, state );
			}
			return state;

		}

		void closeOnFailure() {
			backend.stop();
		}

		BackendNonStartedState getNonStartedState() {
			return new BackendNonStartedState( backendName, backend );
		}
	}

	private static class IndexManagerInitialBuildState implements IndexManagerBuildingState {

		private final String backendName;
		private final String indexName;
		private final IndexManagerBuilder builder;
		private final IndexSchemaRootNodeBuilder schemaRootNodeBuilder;

		private IndexManagerImplementor indexManager;

		IndexManagerInitialBuildState(String backendName, String indexName,
				IndexManagerBuilder builder,
				IndexSchemaRootNodeBuilder schemaRootNodeBuilder) {
			this.backendName = backendName;
			this.indexName = indexName;
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
			return new IndexManagerNonStartedState( backendName, indexName, indexManager );
		}
	}

}
