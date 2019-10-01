/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappedIndexManagerBuilder;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoImplicitReindexingResolverBuildingHelper;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoIndexingDependencyCollectorTypeNode;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexedTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoIndexedTypeManager;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoIndexedTypeManagerContainer;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoTypeAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilterFactory;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.processing.building.impl.PojoIndexingProcessorTypeNodeBuilder;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessor;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class PojoIndexedTypeManagerBuilder<E, D extends DocumentElement> {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoRawTypeModel<E> typeModel;
	private final MappedIndexManagerBuilder<D> indexManagerBuilder;
	private final PojoIndexedTypeExtendedMappingCollector extendedMappingCollector;

	private final PojoIdentityMappingCollectorImpl<E> identityMappingCollector;
	private final PojoIndexingProcessorTypeNodeBuilder<E> processorBuilder;

	private PojoIndexingProcessor<E> preBuiltIndexingProcessor;

	private boolean closed = false;

	PojoIndexedTypeManagerBuilder(PojoRawTypeModel<E> typeModel,
			PojoTypeAdditionalMetadata typeAdditionalMetadata,
			PojoMappingHelper mappingHelper,
			MappedIndexManagerBuilder<D> indexManagerBuilder,
			PojoIndexedTypeExtendedMappingCollector extendedMappingCollector,
			boolean implicitProvidedId) {
		this.typeModel = typeModel;
		this.indexManagerBuilder = indexManagerBuilder;
		this.extendedMappingCollector = extendedMappingCollector;
		this.identityMappingCollector = new PojoIdentityMappingCollectorImpl<>(
				typeModel,
				typeAdditionalMetadata,
				mappingHelper,
				indexManagerBuilder.getRootBindingContext(),
				implicitProvidedId
		);
		this.processorBuilder = new PojoIndexingProcessorTypeNodeBuilder<>(
				BoundPojoModelPath.root( typeModel ),
				mappingHelper, indexManagerBuilder.getRootBindingContext(),
				Optional.of( identityMappingCollector ),
				Collections.emptyList()
		);
	}

	void closeOnFailure() {
		if ( closed ) {
			return;
		}

		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( PojoIndexingProcessorTypeNodeBuilder::closeOnFailure, processorBuilder );
			closer.push( PojoIdentityMappingCollectorImpl::closeOnFailure, identityMappingCollector );
			closer.push( PojoIndexingProcessor::close, preBuiltIndexingProcessor );
			closed = true;
		}
	}

	PojoMappingCollectorTypeNode asCollector() {
		return processorBuilder;
	}

	void preBuild(PojoImplicitReindexingResolverBuildingHelper reindexingResolverBuildingHelper) {
		if ( preBuiltIndexingProcessor != null ) {
			throw new AssertionFailure( "Internal error - preBuild should be called only once" );
		}

		PojoIndexingDependencyCollectorTypeNode<E> dependencyCollector =
				reindexingResolverBuildingHelper.createDependencyCollector( typeModel );
		preBuiltIndexingProcessor = processorBuilder.build( dependencyCollector )
				.orElseGet( PojoIndexingProcessor::noOp );
	}

	void buildAndAddTo(PojoIndexedTypeManagerContainer.Builder typeManagersBuilder,
			PojoImplicitReindexingResolverBuildingHelper reindexingResolverBuildingHelper,
			PojoTypeAdditionalMetadata typeAdditionalMetadata) {
		if ( preBuiltIndexingProcessor == null ) {
			throw new AssertionFailure( "Internal error - preBuild should be called before buildAndAddTo" );
		}

		identityMappingCollector.applyDefaults();

		if ( identityMappingCollector.documentIdSourceProperty.isPresent() ) {
			extendedMappingCollector.documentIdSourceProperty(
					identityMappingCollector.documentIdSourceProperty.get()
			);
		}

		extendedMappingCollector.identifierMapping( identityMappingCollector.identifierMapping );

		/*
		 * TODO offer more flexibility to mapper implementations, allowing them to define their own dirtiness state?
		 * Note this will require to allow them to define their own indexing plan APIs.
		 */
		PojoPathFilterFactory<Set<String>> pathFilterFactory = typeAdditionalMetadata
				.getEntityTypeMetadata().orElseThrow( () -> log.missingEntityTypeMetadata( typeModel ) )
				.getPathFilterFactory();
		Optional<PojoImplicitReindexingResolver<E, Set<String>>> reindexingResolverOptional =
				reindexingResolverBuildingHelper.build( typeModel, pathFilterFactory );

		PojoIndexedTypeManager<?, E, D> typeManager = new PojoIndexedTypeManager<>(
				typeModel.getJavaClass(), typeModel.getCaster(),
				identityMappingCollector.identifierMapping,
				identityMappingCollector.routingKeyProvider,
				preBuiltIndexingProcessor,
				indexManagerBuilder.build(),
				reindexingResolverOptional.orElseGet( PojoImplicitReindexingResolver::noOp )
		);
		log.createdPojoIndexedTypeManager( typeManager );

		typeManagersBuilder.add( typeModel, typeManager );

		closed = true;
	}

}