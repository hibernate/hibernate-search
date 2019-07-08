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

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaRootNodeBuilder;
import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.engine.cfg.impl.EngineConfigurationUtils;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;
import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.backend.spi.BackendFactory;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.backend.spi.BackendBuildContext;
import org.hibernate.search.engine.mapper.mapping.building.impl.IndexedEntityBindingContextImpl;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexManagerBuildingState;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.StringHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.impl.SuppressingCloser;



class IndexManagerBuildingStateHolder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final OptionalConfigurationProperty<String> DEFAULT_INDEX_BACKEND_NAME =
			ConfigurationProperty.forKey( EngineSettings.DEFAULT_BACKEND ).asString().build();

	private static final OptionalConfigurationProperty<BeanReference<? extends BackendFactory>> BACKEND_TYPE =
			ConfigurationProperty.forKey( BackendSettings.TYPE ).asBeanReference( BackendFactory.class )
					.build();

	private final BeanResolver beanResolver;
	private final ConfigurationPropertySource propertySource;
	private final RootBuildContext rootBuildContext;

	// Use a LinkedHashMap for deterministic iteration
	private final Map<String, BackendInitialBuildState<?>> backendBuildStateByName = new LinkedHashMap<>();
	// Use a LinkedHashMap for deterministic iteration
	private final Map<String, IndexManagerInitialBuildState<?>> indexManagerBuildStateByName = new LinkedHashMap<>();

	IndexManagerBuildingStateHolder(BeanResolver beanResolver, ConfigurationPropertySource propertySource,
			RootBuildContext rootBuildContext) {
		this.beanResolver = beanResolver;
		this.propertySource = propertySource;
		this.rootBuildContext = rootBuildContext;
	}

	void createBackends(Set<String> backendNames) {
		if ( backendNames.contains( "" ) || backendNames.contains( null ) ) {
			backendNames.remove( "" );
			backendNames.remove( null );
			backendNames.add( getDefaultBackendName() );
		}
		for ( String backendName : backendNames ) {
			BackendInitialBuildState<?> backendBuildState;
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

	BackendInitialBuildState<?> getBackend(String backendName) {
		if ( StringHelper.isEmpty( backendName ) ) {
			backendName = getDefaultBackendName();
		}
		BackendInitialBuildState<?> backendBuildState = backendBuildStateByName.get( backendName );
		if ( backendBuildState == null ) {
			throw new AssertionFailure(
					"Mapper asking for a reference to backend '" + backendName + "', which was not declared in advance."
					+ " There is a bug in Hibernate Search, please report it."
			);
		}
		return backendBuildState;
	}

	private String getDefaultBackendName() {
		Optional<String> defaultBackendNameOptional = DEFAULT_INDEX_BACKEND_NAME.get( propertySource );
		if ( defaultBackendNameOptional.isPresent() ) {
			return defaultBackendNameOptional.get();
		}
		else {
			throw log.defaultBackendNameNotSet(
					// Retrieve the resolved *default* key (*.default_backend) from the global (non-index-scoped) source
					DEFAULT_INDEX_BACKEND_NAME.resolveOrRaw( propertySource )
			);
		}
	}

	Map<String, BackendPartialBuildState> getBackendPartialBuildStates() {
		// Use a LinkedHashMap for deterministic iteration
		Map<String, BackendPartialBuildState> backendsByName = new LinkedHashMap<>();
		for ( Map.Entry<String, BackendInitialBuildState<?>> entry : backendBuildStateByName.entrySet() ) {
			backendsByName.put( entry.getKey(), entry.getValue().getPartiallyBuilt() );
		}
		return backendsByName;
	}

	Map<String, IndexManagerPartialBuildState> getIndexManagersByName() {
		// Use a LinkedHashMap for deterministic iteration
		Map<String, IndexManagerPartialBuildState> indexManagersByName = new LinkedHashMap<>();
		for ( Map.Entry<String, IndexManagerInitialBuildState<?>> entry : indexManagerBuildStateByName.entrySet() ) {
			indexManagersByName.put( entry.getKey(), entry.getValue().getPartialBuildState() );
		}
		return indexManagersByName;
	}

	void closeOnFailure(SuppressingCloser closer) {
		closer.pushAll( state -> state.closeOnFailure( closer ), indexManagerBuildStateByName.values() );
		closer.pushAll( BackendInitialBuildState::closeOnFailure, backendBuildStateByName.values() );
	}

	private BackendInitialBuildState<?> createBackend(String backendName) {
		ConfigurationPropertySource backendPropertySource =
				EngineConfigurationUtils.getBackend( propertySource, backendName );
		try ( BeanHolder<? extends BackendFactory> backendFactoryHolder =
				BACKEND_TYPE.getAndMapOrThrow(
						backendPropertySource,
						beanResolver::resolve,
						key -> log.backendTypeCannotBeNullOrEmpty( backendName, key )
				) ) {
			BackendBuildContext backendBuildContext = new BackendBuildContextImpl( rootBuildContext );

			BackendImplementor<?> backend = backendFactoryHolder.get()
					.create( backendName, backendBuildContext, backendPropertySource );
			return new BackendInitialBuildState<>( backendName, backendBuildContext, backendPropertySource, backend );
		}
	}

	class BackendInitialBuildState<D extends DocumentElement> {
		private final String backendName;
		private final BackendBuildContext backendBuildContext;
		private final ConfigurationPropertySource backendPropertySource;
		private final ConfigurationPropertySource defaultIndexPropertySource;
		private final BackendImplementor<D> backend;

		private BackendInitialBuildState(
				String backendName,
				BackendBuildContext backendBuildContext,
				ConfigurationPropertySource backendPropertySource,
				BackendImplementor<D> backend) {
			this.backendName = backendName;
			this.backendBuildContext = backendBuildContext;
			this.backendPropertySource = backendPropertySource;
			this.defaultIndexPropertySource =
					EngineConfigurationUtils.getIndexDefaults( backendPropertySource );
			this.backend = backend;
		}

		IndexManagerInitialBuildState<?> getIndexManagerBuildingState(
				String indexName, boolean multiTenancyEnabled) {
			IndexManagerInitialBuildState<?> state = indexManagerBuildStateByName.get( indexName );
			if ( state == null ) {
				ConfigurationPropertySource indexPropertySource =
						EngineConfigurationUtils.getIndex( backendPropertySource, defaultIndexPropertySource, indexName );

				IndexManagerBuilder<D> builder = backend.createIndexManagerBuilder(
						indexName, multiTenancyEnabled, backendBuildContext, indexPropertySource
				);
				IndexSchemaRootNodeBuilder schemaRootNodeBuilder = builder.getSchemaRootNodeBuilder();
				IndexedEntityBindingContext bindingContext = new IndexedEntityBindingContextImpl( schemaRootNodeBuilder );

				state = new IndexManagerInitialBuildState<>( backendName, indexName, builder, bindingContext );
				indexManagerBuildStateByName.put( indexName, state );
			}
			return state;

		}

		void closeOnFailure() {
			backend.close();
		}

		BackendPartialBuildState getPartiallyBuilt() {
			return new BackendPartialBuildState( backendName, backend );
		}
	}

	private class IndexManagerInitialBuildState<D extends DocumentElement> implements IndexManagerBuildingState<D> {

		private final String backendName;
		private final String indexName;
		private final IndexManagerBuilder<D> builder;
		private final IndexedEntityBindingContext bindingContext;

		private IndexManagerImplementor<D> indexManager;

		IndexManagerInitialBuildState(String backendName, String indexName,
				IndexManagerBuilder<D> builder,
				IndexedEntityBindingContext bindingContext) {
			this.backendName = backendName;
			this.indexName = indexName;
			this.builder = builder;
			this.bindingContext = bindingContext;
		}

		void closeOnFailure(SuppressingCloser closer) {
			if ( indexManager != null ) {
				closer.push( IndexManagerImplementor::close, indexManager );
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
		public IndexedEntityBindingContext getIndexedEntityBindingContext() {
			return bindingContext;
		}

		@Override
		public MappedIndexManager<D> build() {
			if ( indexManager != null ) {
				throw new AssertionFailure(
						"Trying to build index manager " + indexName + " twice."
						+ " There is probably a bug in the mapper implementation."
				);
			}
			indexManager = builder.build();
			return new MappedIndexManagerImpl<>( indexManager );
		}

		IndexManagerPartialBuildState getPartialBuildState() {
			if ( indexManager == null ) {
				throw new AssertionFailure(
						"Index manager " + indexName + " was not built by the mapper as expected."
						+ " There is probably a bug in the mapper implementation."
				);
			}
			return new IndexManagerPartialBuildState( backendName, indexName, indexManager );
		}
	}

}
