/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaRootNodeBuilder;
import org.hibernate.search.engine.backend.index.spi.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;
import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.backend.spi.BackendFactory;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.common.spi.BeanProvider;
import org.hibernate.search.engine.backend.spi.BackendBuildContext;
import org.hibernate.search.engine.mapper.mapping.building.impl.RootIndexModelBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexManagerBuildingState;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.util.AssertionFailure;
import org.hibernate.search.util.impl.common.SuppressingCloser;


/**
 * @author Yoann Rodiere
 */
class IndexManagerBuildingStateHolder {

	private static final ConfigurationProperty<Optional<String>> INDEX_BACKEND_NAME =
			ConfigurationProperty.forKey( "backend" ).asString().build();

	private static final ConfigurationProperty<Optional<String>> BACKEND_TYPE =
			ConfigurationProperty.forKey( "type" ).asString().build();

	private final RootBuildContext rootBuildContext;
	private final ConfigurationPropertySource propertySource;
	private final ConfigurationPropertySource defaultIndexPropertySource;

	private final Map<String, BackendBuildingState<?>> backendBuildingStateByName = new HashMap<>();
	private final Map<String, IndexMappingBuildingStateImpl<?>> indexManagerBuildingStateByName = new HashMap<>();

	IndexManagerBuildingStateHolder(RootBuildContext rootBuildContext,
			ConfigurationPropertySource propertySource) {
		this.rootBuildContext = rootBuildContext;
		this.propertySource = propertySource;
		this.defaultIndexPropertySource = propertySource.withMask( "index.default" );
	}

	public IndexManagerBuildingState<?> startBuilding(String indexName, boolean multiTenancyEnabled) {
		ConfigurationPropertySource indexPropertySource = propertySource.withMask( "index." + indexName )
				.withFallback( defaultIndexPropertySource );
		// TODO more checks on the backend name (is non-null, non-empty)
		String backendName = INDEX_BACKEND_NAME.get( indexPropertySource ).get();
		BackendBuildingState<?> backendBuildingstate =
				backendBuildingStateByName.computeIfAbsent( backendName, this::createBackend );

		IndexMappingBuildingStateImpl<?> state = indexManagerBuildingStateByName.get( indexName );
		if ( state == null ) {
			state = backendBuildingstate.createIndexManagerBuildingState(
					indexName, multiTenancyEnabled, indexPropertySource
			);
			indexManagerBuildingStateByName.put( indexName, state );
		}
		return state;
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
		for ( Map.Entry<String, IndexMappingBuildingStateImpl<?>> entry : indexManagerBuildingStateByName.entrySet() ) {
			indexManagersByName.put( entry.getKey(), entry.getValue().getBuilt() );
		}
		return indexManagersByName;
	}

	void closeOnFailure(SuppressingCloser closer) {
		closer.pushAll( state -> state.closeOnFailure( closer ), indexManagerBuildingStateByName.values() );
		closer.pushAll( BackendBuildingState::closeOnFailure, backendBuildingStateByName.values() );
	}

	private BackendBuildingState<?> createBackend(String backendName) {
		ConfigurationPropertySource backendPropertySource = propertySource.withMask( "backend." + backendName );
		// TODO more checks on the backend type (non-null, non-empty)
		String backendType = BACKEND_TYPE.get( backendPropertySource ).get();

		BeanProvider beanProvider = rootBuildContext.getServiceManager().getBeanProvider();
		BackendFactory backendFactory = beanProvider.getBean( backendType, BackendFactory.class );
		BackendBuildContext backendBuildContext = new BackendBuildContextImpl( rootBuildContext );

		BackendImplementor<?> backend = backendFactory.create( backendName, backendBuildContext, backendPropertySource );
		return new BackendBuildingState<>( backendBuildContext, backend );
	}

	private class BackendBuildingState<D extends DocumentElement> {
		private final BackendBuildContext backendBuildContext;
		private final BackendImplementor<D> backend;

		private BackendBuildingState(BackendBuildContext backendBuildContext, BackendImplementor<D> backend) {
			this.backendBuildContext = backendBuildContext;
			this.backend = backend;
		}

		IndexMappingBuildingStateImpl<D> createIndexManagerBuildingState(
				String indexName, boolean multiTenancyEnabled,
				ConfigurationPropertySource indexPropertySource) {
			IndexManagerBuilder<D> builder = backend.createIndexManagerBuilder(
					indexName, multiTenancyEnabled, backendBuildContext, indexPropertySource
			);
			IndexSchemaRootNodeBuilder schemaRootNodeBuilder = builder.getSchemaRootNodeBuilder();
			IndexModelBindingContext bindingContext = new RootIndexModelBindingContext( schemaRootNodeBuilder );
			return new IndexMappingBuildingStateImpl<>( indexName, builder, bindingContext );
		}

		void closeOnFailure() {
			backend.close();
		}

		BackendImplementor<D> getBuilt() {
			return backend;
		}
	}

	private class IndexMappingBuildingStateImpl<D extends DocumentElement> implements IndexManagerBuildingState<D> {

		private final String indexName;
		private final IndexManagerBuilder<D> builder;
		private final IndexModelBindingContext bindingContext;

		private IndexManagerImplementor<D> built;

		IndexMappingBuildingStateImpl(String indexName,
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
		public IndexManager<D> build() {
			if ( built != null ) {
				throw new AssertionFailure(
						"Trying to build index manager " + indexName + " twice."
						+ " There is probably a bug in the mapper implementation."
				);
			}
			built = builder.build();
			return built;
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
