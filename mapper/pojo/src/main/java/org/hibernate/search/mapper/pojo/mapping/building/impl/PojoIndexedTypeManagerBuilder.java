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
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexManagerBuildingState;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.dirtiness.building.impl.PojoImplicitReindexingResolverBuildingHelper;
import org.hibernate.search.mapper.pojo.dirtiness.building.impl.PojoIndexingDependencyCollectorTypeNode;
import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.impl.IdentifierMapping;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoIndexedTypeManager;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoIndexedTypeManagerContainer;
import org.hibernate.search.mapper.pojo.mapping.impl.PropertyIdentifierMapping;
import org.hibernate.search.mapper.pojo.mapping.impl.ProvidedStringIdentifierMapping;
import org.hibernate.search.mapper.pojo.mapping.impl.RoutingKeyBridgeRoutingKeyProvider;
import org.hibernate.search.mapper.pojo.mapping.impl.RoutingKeyProvider;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingTypeMetadata;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoTypeAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilterFactory;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.processing.building.impl.PojoIndexingProcessorTypeNodeBuilder;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessor;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class PojoIndexedTypeManagerBuilder<E, D extends DocumentElement> {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoRawTypeModel<E> typeModel;
	private final PojoTypeAdditionalMetadata typeAdditionalMetadata;
	private final PojoMappingHelper mappingHelper;
	private final IndexManagerBuildingState<D> indexManagerBuildingState;

	private final PojoIdentityMappingCollectorImpl identityMappingCollector;
	private final PojoIndexingProcessorTypeNodeBuilder<E> processorBuilder;

	private PojoIndexingProcessor<E> preBuiltIndexingProcessor;

	private boolean closed = false;

	PojoIndexedTypeManagerBuilder(PojoRawTypeModel<E> typeModel,
			PojoTypeAdditionalMetadata typeAdditionalMetadata,
			PojoMappingHelper mappingHelper,
			IndexManagerBuildingState<D> indexManagerBuildingState,
			boolean implicitProvidedId) {
		this.typeModel = typeModel;
		this.typeAdditionalMetadata = typeAdditionalMetadata;
		this.mappingHelper = mappingHelper;
		this.indexManagerBuildingState = indexManagerBuildingState;
		this.identityMappingCollector = new PojoIdentityMappingCollectorImpl(
				implicitProvidedId
		);
		IndexBindingContext bindingContext = indexManagerBuildingState.getIndexedEntityBindingContext();
		this.processorBuilder = new PojoIndexingProcessorTypeNodeBuilder<>(
				BoundPojoModelPath.root( typeModel ),
				mappingHelper, bindingContext,
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

		/*
		 * TODO offer more flexibility to mapper implementations, allowing them to define their own dirtiness state?
		 * Note this will require to allow them to define their own work plan APIs.
		 */
		PojoPathFilterFactory<Set<String>> pathFilterFactory = typeAdditionalMetadata
				.getEntityTypeMetadata().orElseThrow( () -> log.missingEntityTypeMetadata( typeModel ) )
				.getPathFilterFactory();
		Optional<PojoImplicitReindexingResolver<E, Set<String>>> reindexingResolverOptional =
				reindexingResolverBuildingHelper.build( typeModel, pathFilterFactory );

		PojoIndexedTypeManager<?, E, D> typeManager = new PojoIndexedTypeManager<>(
				typeModel.getJavaClass(), typeModel.getCaster(),
				new PojoMappingTypeMetadata(
						identityMappingCollector.documentIdMappedToEntityId,
						identityMappingCollector.documentIdSourcePropertyName
				),
				identityMappingCollector.identifierMapping,
				identityMappingCollector.routingKeyProvider,
				preBuiltIndexingProcessor,
				indexManagerBuildingState.build(),
				reindexingResolverOptional.orElseGet( PojoImplicitReindexingResolver::noOp )
		);
		log.createdPojoIndexedTypeManager( typeManager );

		typeManagersBuilder.add( indexManagerBuildingState.getIndexName(), typeModel, typeManager );

		closed = true;
	}

	private class PojoIdentityMappingCollectorImpl implements PojoIdentityMappingCollector {
		private final boolean implicitProvidedId;
		private final BoundPojoModelPathPropertyNode<?, ?> entityIdPropertyPath;

		private IdentifierMapping<?, E> identifierMapping;
		private boolean documentIdMappedToEntityId;
		private Optional<String> documentIdSourcePropertyName;
		private RoutingKeyProvider<E> routingKeyProvider;

		PojoIdentityMappingCollectorImpl(boolean implicitProvidedId) {
			this.implicitProvidedId = implicitProvidedId;

			Optional<String> entityIdPropertyName = typeAdditionalMetadata.getEntityTypeMetadata()
					.orElseThrow( () -> log.missingEntityTypeMetadata( typeModel ) )
					.getEntityIdPropertyName();
			if ( entityIdPropertyName.isPresent() ) {
				this.entityIdPropertyPath = BoundPojoModelPath.root( typeModel ).property( entityIdPropertyName.get() );
			}
			else {
				this.entityIdPropertyPath = null;
			}
		}

		void closeOnFailure() {
			try ( Closer<RuntimeException> closer = new Closer<>() ) {
				closer.push( IdentifierMapping::close, identifierMapping );
				closer.push( RoutingKeyProvider::close, routingKeyProvider );
			}
		}

		@Override
		public <T> void identifierBridge(BoundPojoModelPathPropertyNode<?, T> modelPath,
				BridgeBuilder<? extends IdentifierBridge<?>> builder) {
			BeanHolder<? extends IdentifierBridge<T>> bridgeHolder = mappingHelper.getIndexModelBinder()
					.addIdentifierBridge( indexManagerBuildingState.getIndexedEntityBindingContext(), modelPath, builder );
			PojoPropertyModel<T> propertyModel = modelPath.getPropertyModel();
			this.identifierMapping = new PropertyIdentifierMapping<>(
					propertyModel.getTypeModel().getRawType().getCaster(),
					propertyModel.getHandle(),
					bridgeHolder
			);
			this.documentIdSourcePropertyName = Optional.of( propertyModel.getName() );
			this.documentIdMappedToEntityId =
					entityIdPropertyPath == null
							? false
							: entityIdPropertyPath.toUnboundPath().equals( modelPath.toUnboundPath() );
		}

		@Override
		public <T> BoundRoutingKeyBridge<T> routingKeyBridge(BoundPojoModelPathTypeNode<T> modelPath,
				BridgeBuilder<? extends RoutingKeyBridge> builder) {
			BoundRoutingKeyBridge<T> boundRoutingKeyBridge = mappingHelper.getIndexModelBinder()
					.addRoutingKeyBridge( indexManagerBuildingState.getIndexedEntityBindingContext(), modelPath, builder );
			this.routingKeyProvider = new RoutingKeyBridgeRoutingKeyProvider<>( boundRoutingKeyBridge.getBridgeHolder() );
			return boundRoutingKeyBridge;
		}

		void applyDefaults() {
			if ( identifierMapping == null ) {
				// Assume a provided ID if requested
				if ( implicitProvidedId ) {
					identifierMapping = ProvidedStringIdentifierMapping.get();
					documentIdSourcePropertyName = Optional.empty();
					documentIdMappedToEntityId = false;
				}
				// Fall back to the entity ID if possible
				else if ( entityIdPropertyPath != null ) {
					identifierBridge( entityIdPropertyPath, null );
				}
				else {
					throw log.missingIdentifierMapping( typeModel );
				}
			}

			if ( routingKeyProvider == null ) {
				routingKeyProvider = RoutingKeyProvider.alwaysNull();
			}
		}

	}
}