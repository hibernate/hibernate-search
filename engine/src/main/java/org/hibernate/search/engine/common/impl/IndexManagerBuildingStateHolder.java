/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaRootNodeBuilder;
import org.hibernate.search.engine.cfg.SearchEngineSettings;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;
import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.backend.spi.BackendFactory;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanProvider;
import org.hibernate.search.engine.backend.spi.BackendBuildContext;
import org.hibernate.search.engine.mapper.mapping.building.impl.RootIndexModelBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexManagerBuildingState;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.util.AssertionFailure;
import org.hibernate.search.util.impl.common.LoggerFactory;
import org.hibernate.search.util.impl.common.SuppressingCloser;


/**
 * @author Yoann Rodiere
 */
class IndexManagerBuildingStateHolder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final OptionalConfigurationProperty<String> DEFAULT_INDEX_BACKEND_NAME =
			ConfigurationProperty.forKey( SearchEngineSettings.DEFAULT_BACKEND ).asString().build();

	private static final OptionalConfigurationProperty<String> INDEX_BACKEND_NAME =
			ConfigurationProperty.forKey( "backend" ).asString().build();

	private static final OptionalConfigurationProperty<BeanReference<? extends BackendFactory>> BACKEND_TYPE =
			ConfigurationProperty.forKey( "type" ).asBeanReference( BackendFactory.class ).build();

	private final BeanProvider beanProvider;
	private final ConfigurationPropertySource propertySource;
	private final RootBuildContext rootBuildContext;

	private final Map<String, BackendBuildingState<?>> backendBuildingStateByName = new HashMap<>();
	private final Map<String, IndexManagerBuildingStateImpl<?>> indexManagerBuildingStateByName = new HashMap<>();

	IndexManagerBuildingStateHolder(BeanProvider beanProvider, ConfigurationPropertySource propertySource,
			RootBuildContext rootBuildContext) {
		this.beanProvider = beanProvider;
		this.propertySource = propertySource;
		this.rootBuildContext = rootBuildContext;
	}

	public IndexManagerBuildingState<?> startBuilding(String indexName, boolean multiTenancyEnabled) {
		ConfigurationPropertySource indexPropertySource = propertySource.withMask( "indexes." + indexName );
		String backendName = getBackendName( indexName, indexPropertySource );

		BackendBuildingState<?> backendBuildingstate =
				backendBuildingStateByName.computeIfAbsent( backendName, this::createBackend );

		IndexManagerBuildingStateImpl<?> state = indexManagerBuildingStateByName.get( indexName );
		if ( state == null ) {
			state = backendBuildingstate.createIndexManagerBuildingState(
					indexName, multiTenancyEnabled, indexPropertySource
			);
			indexManagerBuildingStateByName.put( indexName, state );
		}
		return state;
	}

	private String getBackendName(String indexName, ConfigurationPropertySource indexPropertySource) {
		Optional<String> backendNameOptional = INDEX_BACKEND_NAME.get( indexPropertySource );
		if ( backendNameOptional.isPresent() ) {
			return backendNameOptional.get();
		}
		else {
			Optional<String> defaultBackendNameOptional = DEFAULT_INDEX_BACKEND_NAME.get( propertySource );
			if ( defaultBackendNameOptional.isPresent() ) {
				return defaultBackendNameOptional.get();
			}
			else {
				throw log.indexBackendCannotBeNullOrEmpty(
						indexName,
						INDEX_BACKEND_NAME.resolveOrRaw( indexPropertySource ),
						// Retrieve the resolved *default* key (*.default_backend) from the global (non-index-scoped) source
						DEFAULT_INDEX_BACKEND_NAME.resolveOrRaw( propertySource )
				);
			}
		}
	}

	Map<String, BackendImplementor<?>> getBackendsByName() {
		Map<String, BackendImplementor<?>> backendsByName = new HashMap<>();
		for ( Map.Entry<String, BackendBuildingState<?>> entry : backendBuildingStateByName.entrySet() ) {
			backendsByName.put( entry.getKey(), entry.getValue().getBuilt() );
		}
		return backendsByName;
	}

	Map<String, IndexManagerImplementor<?>> getIndexManagersByName() {
		Map<String, IndexManagerImplementor<?>> indexManagersByName = new HashMap<>();
		for ( Map.Entry<String, IndexManagerBuildingStateImpl<?>> entry : indexManagerBuildingStateByName.entrySet() ) {
			indexManagersByName.put( entry.getKey(), entry.getValue().getBuilt() );
		}
		return indexManagersByName;
	}

	void closeOnFailure(SuppressingCloser closer) {
		closer.pushAll( state -> state.closeOnFailure( closer ), indexManagerBuildingStateByName.values() );
		closer.pushAll( BackendBuildingState::closeOnFailure, backendBuildingStateByName.values() );
	}

	private BackendBuildingState<?> createBackend(String backendName) {
		ConfigurationPropertySource backendPropertySource = propertySource.withMask( "backends." + backendName );
		try ( BeanHolder<? extends BackendFactory> backendFactoryHolder =
				BACKEND_TYPE.getAndMapOrThrow(
						backendPropertySource,
						beanProvider::getBean,
						key -> log.backendTypeCannotBeNullOrEmpty( backendName, key )
				) ) {
			BackendBuildContext backendBuildContext = new BackendBuildContextImpl( rootBuildContext );

			BackendImplementor<?> backend = backendFactoryHolder.get()
					.create( backendName, backendBuildContext, backendPropertySource );
			return new BackendBuildingState<>( backendBuildContext, backendPropertySource, backend );
		}
	}

	private class BackendBuildingState<D extends DocumentElement> {
		private final BackendBuildContext backendBuildContext;
		private final ConfigurationPropertySource defaultIndexPropertySource;
		private final BackendImplementor<D> backend;

		private BackendBuildingState(BackendBuildContext backendBuildContext,
				ConfigurationPropertySource backendPropertySource,
				BackendImplementor<D> backend) {
			this.backendBuildContext = backendBuildContext;
			this.defaultIndexPropertySource = backendPropertySource.withMask( "index_defaults" );
			this.backend = backend;
		}

		IndexManagerBuildingStateImpl<D> createIndexManagerBuildingState(
				String indexName, boolean multiTenancyEnabled,
				ConfigurationPropertySource indexPropertySource) {
			ConfigurationPropertySource defaultedIndexPropertySource =
					indexPropertySource.withFallback( defaultIndexPropertySource );
			IndexManagerBuilder<D> builder = backend.createIndexManagerBuilder(
					indexName, multiTenancyEnabled, backendBuildContext, defaultedIndexPropertySource
			);
			IndexSchemaRootNodeBuilder schemaRootNodeBuilder = builder.getSchemaRootNodeBuilder();
			IndexModelBindingContext bindingContext = new RootIndexModelBindingContext( schemaRootNodeBuilder );
			return new IndexManagerBuildingStateImpl<>( indexName, builder, bindingContext );
		}

		void closeOnFailure() {
			backend.close();
		}

		BackendImplementor<D> getBuilt() {
			return backend;
		}
	}

	private class IndexManagerBuildingStateImpl<D extends DocumentElement> implements IndexManagerBuildingState<D> {

		private final String indexName;
		private final IndexManagerBuilder<D> builder;
		private final IndexModelBindingContext bindingContext;

		private IndexManagerImplementor<D> built;

		IndexManagerBuildingStateImpl(String indexName,
				IndexManagerBuilder<D> builder,
				IndexModelBindingContext bindingContext) {
			this.indexName = indexName;
			this.builder = builder;
			this.bindingContext = bindingContext;
		}

		void closeOnFailure(SuppressingCloser closer) {
			if ( built != null ) {
				closer.push( IndexManagerImplementor::close, built );
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
		public IndexModelBindingContext getRootBindingContext() {
			return bindingContext;
		}

		@Override
		public MappedIndexManager<D> build() {
			if ( built != null ) {
				throw new AssertionFailure(
						"Trying to build index manager " + indexName + " twice."
						+ " There is probably a bug in the mapper implementation."
				);
			}
			built = builder.build();
			return new MappedIndexManagerImpl<>( built );
		}

		public IndexManagerImplementor<D> getBuilt() {
			if ( built == null ) {
				throw new AssertionFailure(
						"Index manager " + indexName + " was not built by the mapper as expected."
						+ " There is probably a bug in the mapper implementation."
				);
			}
			return built;
		}
	}

}
