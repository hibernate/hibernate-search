/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.search.engine.backend.document.model.spi.IndexModelCollectorImplementor;
import org.hibernate.search.engine.backend.document.spi.DocumentState;
import org.hibernate.search.engine.backend.index.impl.SimplifyingIndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;
import org.hibernate.search.engine.backend.spi.Backend;
import org.hibernate.search.engine.backend.spi.BackendFactory;
import org.hibernate.search.engine.bridge.impl.BridgeFactory;
import org.hibernate.search.engine.bridge.impl.BridgeReferenceResolver;
import org.hibernate.search.engine.common.spi.BeanResolver;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.engine.mapper.mapping.building.impl.MappingIndexModelCollectorImpl;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexManagerBuildingState;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingIndexModelCollector;
import org.hibernate.search.engine.mapper.model.spi.IndexableTypeOrdering;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.spi.MaskedProperty;


/**
 * @author Yoann Rodiere
 */
// TODO close every backend built so far (which should close index managers) in case of failure
public class IndexManagerBuildingStateHolder {

	private final BuildContext buildContext;
	private final Properties properties;
	private final Properties defaultIndexProperties;
	private final BridgeFactory bridgeFactory;
	private final BridgeReferenceResolver bridgeReferenceResolver;

	private final Map<String, Backend<?>> backendsByName = new HashMap<>();
	private final Map<String, IndexMappingBuildingStateImpl<?>> indexManagerBuildingStateByName = new HashMap<>();

	public IndexManagerBuildingStateHolder(BuildContext buildContext,
			Properties properties, BridgeFactory bridgeFactory,
			BridgeReferenceResolver bridgeReferenceResolver) {
		this.buildContext = buildContext;
		this.properties = properties;
		this.defaultIndexProperties = new MaskedProperty( properties, "index.default" );
		this.bridgeFactory = bridgeFactory;
		this.bridgeReferenceResolver = bridgeReferenceResolver;
	}

	public IndexManagerBuildingState<?> startBuilding(String indexName, IndexableTypeOrdering typeOrdering) {
		return indexManagerBuildingStateByName.compute(
				indexName,
				(k, v) -> {
					if ( v == null ) {
						/*
						 * TODO maybe remove this "index" prefix, to limit the disruption for users migrating from 5.x?
						 * The downside is we may have name conflicts with other properties (most notably, backend definitions).
						 */
						Properties indexProperties = new MaskedProperty( properties, "index." + indexName, defaultIndexProperties );
						return createIndexManagerBuildingState( indexName, indexProperties, typeOrdering );
					}
					else {
						/*
						 * TODO decide if forbidding multiple entity mappings to target the same index is really what we want.
						 * It sure as hell is the easiest solution for Elasticsearch 6,
						 * because it means we won't need to handle the merging of multiple mappings
						 * into a single index.
						 * But it also means we won't be able to map multiple entities in the same hierarchy tree
						 * to a single index.
						 */
						throw new SearchException( "Multiple entity mappings target the same index, which is forbidden" );
					}
				} );
	}

	private IndexMappingBuildingStateImpl<?> createIndexManagerBuildingState(
			String indexName, Properties indexProperties, IndexableTypeOrdering typeOrdering) {
		// TODO more checks on the backend name (is non-null, non-empty)
		String backendName = indexProperties.getProperty( "backend" );
		Backend<?> backend = backendsByName.computeIfAbsent( backendName, this::createBackend );
		return createIndexManagerBuildingState( backend, indexName, indexProperties, typeOrdering );
	}

	private <D extends DocumentState> IndexMappingBuildingStateImpl<D> createIndexManagerBuildingState(
			Backend<D> backend, String indexName, Properties indexProperties, IndexableTypeOrdering typeOrdering) {
		IndexManagerBuilder<D> builder = backend.createIndexManagerBuilder( indexName, buildContext, indexProperties );
		IndexModelCollectorImplementor modelCollector = builder.getModelCollector();
		MappingIndexModelCollectorImpl mappingModelCollector = new MappingIndexModelCollectorImpl(
				bridgeFactory, bridgeReferenceResolver, modelCollector, typeOrdering );
		return new IndexMappingBuildingStateImpl<>( builder, mappingModelCollector );
	}

	private Backend<?> createBackend(String backendName) {
		Properties backendProperties = new MaskedProperty( properties, "backend." + backendName );
		// TODO more checks on the backend type (non-null, non-empty)
		String backendType = backendProperties.getProperty( "type" );

		/*
		 * TODO use a more easily extensible approach.
		 * For instance introduce extension points to register aliases for each backend factory.
		 * Aliases would first be resolved, and only then the result of the resolution would
		 * be passed to the bean resolver.
		 */
		BeanResolver beanResolver = buildContext.getServiceManager().getBeanResolver();
		BackendFactory backendFactory = beanResolver.resolve( backendType, BackendFactory.class );
		return backendFactory.create( backendName, buildContext, backendProperties );
	}

	public Map<String, IndexManager<?>> build() {
		Map<String, IndexManager<?>> result = new HashMap<>();
		indexManagerBuildingStateByName.forEach( (k, v) -> result.put( k, v.build() ) );
		// TODO close the managers created so far if anything fails
		return result;
	}

	private static class IndexMappingBuildingStateImpl<D extends DocumentState> implements IndexManagerBuildingState<D> {

		private final IndexManagerBuilder<D> builder;
		private final MappingIndexModelCollector modelCollector;
		private IndexManager<D> result;

		public IndexMappingBuildingStateImpl(IndexManagerBuilder<D> builder,
				MappingIndexModelCollector modelCollector) {
			this.builder = builder;
			this.modelCollector = modelCollector;
		}

		@Override
		public MappingIndexModelCollector getModelCollector() {
			return modelCollector;
		}

		@Override
		public IndexManager<D> getResult() {
			if ( result == null ) {
				throw new SearchException( "getResult() called before the IndexManager was built" );
			}
			return result;
		}

		public IndexManager<D> build() {
			result = builder.build();
			// Optimize changeset execution in the resulting index manager
			result = new SimplifyingIndexManager<>( result );
			return result;
		}
	}

}
