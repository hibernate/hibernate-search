/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEmbeddedDefinition;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEmbeddedPathTracker;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingMapperContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappedIndexManagerBuilder;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappedIndexManagerFactory;
import org.hibernate.search.engine.mapper.mapping.building.spi.Mapper;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingAbortedException;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingBuildContext;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.reporting.spi.EventContexts;

class StubMapper implements Mapper<StubMappingPartialBuildState>, IndexedEntityBindingMapperContext {

	private final ContextualFailureCollector failureCollector;
	private final TypeMetadataContributorProvider<StubTypeMetadataContributor> contributorProvider;

	private final boolean multiTenancyEnabled;

	private final Map<StubTypeModel, MappedIndexManagerBuilder<?>> indexManagerBuilders = new HashMap<>();
	private final Map<IndexedEmbeddedDefinition, IndexedEmbeddedPathTracker> pathTrackers = new HashMap<>();

	StubMapper(MappingBuildContext buildContext,
			TypeMetadataContributorProvider<StubTypeMetadataContributor> contributorProvider,
			boolean multiTenancyEnabled) {
		this.failureCollector = buildContext.getFailureCollector();
		this.contributorProvider = contributorProvider;
		this.multiTenancyEnabled = multiTenancyEnabled;
	}

	@Override
	public void closeOnFailure() {
		// Nothing to do
	}

	@Override
	public void prepareIndexedTypes(Consumer<Optional<String>> backendNameCollector) {
		contributorProvider.getTypesContributedTo()
				.forEach( type -> {
					try {
						prepareType( type, backendNameCollector );
					}
					catch (RuntimeException e) {
						failureCollector.withContext( EventContexts.fromType( type ) )
								.add( e );
					}
				} );
	}

	private void prepareType(MappableTypeModel type, Consumer<Optional<String>> backendNameCollector) {
		Set<StubTypeMetadataContributor> metadataSet = contributorProvider.get( type );
		metadataSet.forEach( metadata -> {
			if ( metadata.getIndexName() != null ) {
				backendNameCollector.accept( Optional.ofNullable( metadata.getBackendName() ) );
			}
		} );
	}

	@Override
	public void mapIndexedTypes(MappedIndexManagerFactory indexManagerFactory) {
		contributorProvider.getTypesContributedTo()
				.forEach( type -> {
					try {
						mapTypeIfIndexed( type, indexManagerFactory );
					}
					catch (RuntimeException e) {
						failureCollector.withContext( EventContexts.fromType( type ) )
								.add( e );
					}
				} );
	}

	private void mapTypeIfIndexed(MappableTypeModel type,
			MappedIndexManagerFactory indexManagerFactory) {
		Set<StubTypeMetadataContributor> contributorSet = contributorProvider.get( type );
		String indexName = null;
		String backendName = null;
		for ( StubTypeMetadataContributor contributor : contributorSet ) {
			if ( contributor.getIndexName() != null ) {
				indexName = contributor.getIndexName();
			}
			if ( contributor.getBackendName() != null ) {
				backendName = contributor.getBackendName();
			}
		}
		if ( indexName != null ) {
			MappedIndexManagerBuilder<?> indexManagerBuilder = indexManagerFactory.createMappedIndexManager(
					this,
					Optional.ofNullable( backendName ),
					indexName,
					type.getName(),
					multiTenancyEnabled
			);
			indexManagerBuilders.put( (StubTypeModel) type, indexManagerBuilder );
			contributorProvider.get( type ).forEach( c -> c.contribute( indexManagerBuilder.getRootBindingContext() ) );
		}
	}

	@Override
	public StubMappingPartialBuildState prepareBuild() throws MappingAbortedException {
		Map<String, StubMappingIndexManager> indexMappingsByTypeIdentifier = new HashMap<>();
		for ( Map.Entry<StubTypeModel, MappedIndexManagerBuilder<?>> entry : indexManagerBuilders.entrySet() ) {
			StubTypeModel typeModel = entry.getKey();
			try {
				MappedIndexManager<?> indexManager = entry.getValue().build();
				indexMappingsByTypeIdentifier.put(
						typeModel.asString(),
						new StubMappingIndexManager( indexManager )
				);
			}
			catch (RuntimeException e) {
				failureCollector.withContext( EventContexts.fromType( typeModel ) ).add( e );
			}
		}

		if ( failureCollector.hasFailure() ) {
			throw new MappingAbortedException();
		}

		return new StubMappingPartialBuildState( indexMappingsByTypeIdentifier );
	}

	@Override
	public IndexedEmbeddedPathTracker getOrCreatePathTracker(IndexedEmbeddedDefinition definition) {
		return pathTrackers.computeIfAbsent( definition, IndexedEmbeddedPathTracker::new );
	}
}
