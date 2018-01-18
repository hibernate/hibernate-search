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
import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaCollector;
import org.hibernate.search.engine.backend.index.impl.SimplifyingIndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;
import org.hibernate.search.engine.backend.spi.Backend;
import org.hibernate.search.engine.backend.spi.BackendFactory;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.common.spi.BeanResolver;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.engine.mapper.mapping.building.impl.IndexModelBindingContextImpl;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexManagerBuildingState;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.engine.mapper.model.spi.IndexableTypeOrdering;
import org.hibernate.search.util.SearchException;


/**
 * @author Yoann Rodiere
 */
// TODO close every backend built so far (which should close index managers) in case of failure
public class IndexManagerBuildingStateHolder {

	private static final ConfigurationProperty<Optional<String>> INDEX_BACKEND_NAME =
			ConfigurationProperty.forKey( "backend" ).asString().build();

	private static final ConfigurationProperty<Optional<String>> BACKEND_TYPE =
			ConfigurationProperty.forKey( "type" ).asString().build();

	private final BuildContext buildContext;
	private final ConfigurationPropertySource propertySource;
	private final ConfigurationPropertySource defaultIndexPropertySource;

	private final Map<String, Backend<?>> backendsByName = new HashMap<>();
	private final Map<String, IndexMappingBuildingStateImpl<?>> indexManagerBuildingStateByName = new HashMap<>();

	public IndexManagerBuildingStateHolder(BuildContext buildContext,
			ConfigurationPropertySource propertySource) {
		this.buildContext = buildContext;
		this.propertySource = propertySource;
		this.defaultIndexPropertySource = propertySource.withMask( "index.default" );
	}

	public IndexManagerBuildingState<?> startBuilding(String indexName, IndexableTypeOrdering typeOrdering) {
		return indexManagerBuildingStateByName.compute(
				indexName,
				(k, v) -> {
					if ( v == null ) {
						ConfigurationPropertySource indexPropertySource = propertySource.withMask("index." + indexName )
								.withFallback( defaultIndexPropertySource );
						return createIndexManagerBuildingState( indexName, indexPropertySource, typeOrdering );
					}
					else {
						throw new SearchException( "Multiple entity mappings target the same index, which is forbidden" );
					}
				} );
	}

	private IndexMappingBuildingStateImpl<?> createIndexManagerBuildingState(
			String indexName, ConfigurationPropertySource indexPropertySource, IndexableTypeOrdering typeOrdering) {
		// TODO more checks on the backend name (is non-null, non-empty)
		String backendName = INDEX_BACKEND_NAME.get( indexPropertySource ).get();
		Backend<?> backend = backendsByName.computeIfAbsent( backendName, this::createBackend );
		return createIndexManagerBuildingState( backend, indexName, indexPropertySource, typeOrdering );
	}

	private <D extends DocumentElement> IndexMappingBuildingStateImpl<D> createIndexManagerBuildingState(
			Backend<D> backend, String indexName, ConfigurationPropertySource indexPropertySource,
			IndexableTypeOrdering typeOrdering) {
		IndexManagerBuilder<D> builder = backend.createIndexManagerBuilder( indexName, buildContext, indexPropertySource );
		IndexSchemaCollector schemaCollector = builder.getSchemaCollector();
		IndexModelBindingContext bindingContext = new IndexModelBindingContextImpl( schemaCollector, typeOrdering );
		return new IndexMappingBuildingStateImpl<>( indexName, builder, bindingContext );
	}

	private Backend<?> createBackend(String backendName) {
		ConfigurationPropertySource backendPropertySource = propertySource.withMask( "backend." + backendName );
		// TODO more checks on the backend type (non-null, non-empty)
		String backendType = BACKEND_TYPE.get( backendPropertySource ).get();

		BeanResolver beanResolver = buildContext.getServiceManager().getBeanResolver();
		BackendFactory backendFactory = beanResolver.resolve( backendType, BackendFactory.class );
		return backendFactory.create( backendName, buildContext, backendPropertySource );
	}

	private static class IndexMappingBuildingStateImpl<D extends DocumentElement> implements IndexManagerBuildingState<D> {

		private final String indexName;
		private final IndexManagerBuilder<D> builder;
		private final IndexModelBindingContext bindingContext;

		public IndexMappingBuildingStateImpl(String indexName,
				IndexManagerBuilder<D> builder,
				IndexModelBindingContext bindingContext) {
			this.indexName = indexName;
			this.builder = builder;
			this.bindingContext = bindingContext;
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
			IndexManager<D> result = builder.build();
			// Optimize changeset execution in the resulting index manager
			result = new SimplifyingIndexManager<>( result );
			return result;
		}
	}

}
