/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.backend.mapping.spi.BackendMapperContext;
import org.hibernate.search.engine.backend.reporting.spi.BackendMappingHints;
import org.hibernate.search.engine.common.tree.TreeFilterDefinition;
import org.hibernate.search.engine.common.tree.spi.TreeFilterPathTracker;
import org.hibernate.search.engine.mapper.mapping.building.spi.BackendsInfo;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingMapperContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappedIndexManagerBuilder;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappedIndexManagerFactory;
import org.hibernate.search.engine.mapper.mapping.building.spi.Mapper;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingAbortedException;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingBuildContext;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.engine.mapper.model.spi.MappingElement;
import org.hibernate.search.engine.mapper.model.spi.TypeMetadataContributorProvider;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.tenancy.spi.TenancyMode;

class StubMapper implements Mapper<StubMappingPartialBuildState>, IndexedEntityBindingMapperContext, BackendMapperContext {

	private final ContextualFailureCollector failureCollector;
	private final TypeMetadataContributorProvider<StubMappedIndex> contributorProvider;

	private final StubMappingBackendFeatures backendFeatures;
	private final TenancyMode tenancyMode;

	private final Map<StubTypeModel, MappedIndexManagerBuilder> indexManagerBuilders = new HashMap<>();
	private final Map<MappingElement, TreeFilterPathTracker> pathTrackers = new HashMap<>();

	StubMapper(MappingBuildContext buildContext,
			TypeMetadataContributorProvider<StubMappedIndex> contributorProvider,
			StubMappingBackendFeatures backendFeatures,
			TenancyMode tenancyMode) {
		this.failureCollector = buildContext.failureCollector();
		this.contributorProvider = contributorProvider;
		this.backendFeatures = backendFeatures;
		this.tenancyMode = tenancyMode;
	}

	@Override
	public void closeOnFailure() {
		// Nothing to do
	}

	@Override
	public void prepareMappedTypes(BackendsInfo backendsInfo) {
		contributorProvider.typesContributedTo()
				.forEach( type -> {
					try {
						prepareType( type, backendsInfo );
					}
					catch (RuntimeException e) {
						failureCollector.withContext( EventContexts.fromType( type ) )
								.add( e );
					}
				} );
	}

	private void prepareType(MappableTypeModel type, BackendsInfo backendsInfo) {
		getMappedIndex( type )
				.ifPresent( mappedIndex -> backendsInfo.collect( mappedIndex.backendName(), tenancyMode ) );
	}

	@Override
	public void mapTypes(MappedIndexManagerFactory indexManagerFactory) {
		contributorProvider.typesContributedTo()
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

	private void mapTypeIfIndexed(MappableTypeModel type, MappedIndexManagerFactory indexManagerFactory) {
		Optional<StubMappedIndex> mappedIndexOptional = getMappedIndex( type );
		mappedIndexOptional.ifPresent( mappedIndex -> {
			MappedIndexManagerBuilder indexManagerBuilder = indexManagerFactory.createMappedIndexManager(
					this,
					this,
					mappedIndex.backendName(),
					mappedIndex.name(),
					type.name()
			);
			indexManagerBuilders.put( (StubTypeModel) type, indexManagerBuilder );
			mappedIndex.bind( indexManagerBuilder.rootBindingContext() );
		} );
	}

	private Optional<StubMappedIndex> getMappedIndex(MappableTypeModel type) {
		Set<StubMappedIndex> stubMappedIndices = contributorProvider.get( type );
		if ( stubMappedIndices.isEmpty() ) {
			return Optional.empty();
		}
		if ( stubMappedIndices.size() > 1 ) {
			throw new IllegalStateException( "Multiple type mappings for type " + type
					+ ". Only one mapping per type is not supported for the stub mapper." );
		}
		return Optional.of( stubMappedIndices.iterator().next() );
	}

	@Override
	public StubMappingPartialBuildState prepareBuild() throws MappingAbortedException {
		Map<String, StubMappedIndex> mappedIndexesByTypeIdentifier = new HashMap<>();
		for ( Map.Entry<StubTypeModel, MappedIndexManagerBuilder> entry : indexManagerBuilders.entrySet() ) {
			StubTypeModel typeModel = entry.getKey();
			try {
				MappedIndexManager indexManagerDelegate = entry.getValue().build();
				StubMappedIndex mappedIndex = getMappedIndex( typeModel ).get();
				mappedIndex.onIndexManagerCreated( indexManagerDelegate );

				mappedIndexesByTypeIdentifier.put( typeModel.asString(), mappedIndex );
			}
			catch (RuntimeException e) {
				failureCollector.withContext( EventContexts.fromType( typeModel ) ).add( e );
			}
		}

		if ( failureCollector.hasFailure() ) {
			throw new MappingAbortedException();
		}

		return new StubMappingPartialBuildState( backendFeatures, mappedIndexesByTypeIdentifier );
	}

	@Override
	public TreeFilterPathTracker getOrCreatePathTracker(MappingElement mappingElement,
			TreeFilterDefinition filterDefinition) {
		TreeFilterPathTracker result = pathTrackers.get( mappingElement );
		if ( result != null ) {
			return result;
		}
		result = new TreeFilterPathTracker( filterDefinition );
		pathTrackers.put( mappingElement, result );
		return result;
	}

	@Override
	public BackendMappingHints hints() {
		return StubMappingHints.INSTANCE;
	}
}
