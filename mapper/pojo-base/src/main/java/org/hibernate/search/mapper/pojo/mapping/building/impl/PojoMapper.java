/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.engine.mapper.mapping.building.spi.BackendsInfo;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappedIndexManagerBuilder;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappedIndexManagerFactory;
import org.hibernate.search.engine.mapper.mapping.building.spi.Mapper;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingAbortedException;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingBuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingPartialBuildState;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.engine.mapper.model.spi.TypeMetadataContributorProvider;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.tenancy.spi.TenancyMode;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoImplicitReindexingResolverBuildingHelper;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.BoundRoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.RoutingBindingContextImpl;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.BridgeResolver;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingBinder;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorBinder;
import org.hibernate.search.mapper.pojo.identity.impl.IdentityMappingMode;
import org.hibernate.search.mapper.pojo.identity.impl.PojoRootIdentityMappingCollector;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMapperDelegate;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.impl.AbstractPojoTypeManager;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoIndexedTypeManager;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoMappingDelegateImpl;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoTypeManagerContainer;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl.PojoTypeAdditionalMetadataProvider;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoEntityTypeAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoIndexedTypeAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoTypeAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.dependency.impl.PojoRoutingIndexingDependencyConfigurationContextImpl;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelTypeRootElement;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPath;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.processing.building.impl.PojoIndexingProcessorOriginalTypeNodeBuilder;
import org.hibernate.search.mapper.pojo.reporting.spi.PojoEventContexts;
import org.hibernate.search.mapper.pojo.search.definition.impl.PojoSearchQueryElementRegistry;
import org.hibernate.search.mapper.pojo.search.definition.impl.PojoSearchQueryElementRegistryBuilder;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class PojoMapper<MPBS extends MappingPartialBuildState> implements Mapper<MPBS> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ContextualFailureCollector failureCollector;
	private final TypeMetadataContributorProvider<PojoTypeMetadataContributor> contributorProvider;
	private final BeanReference<? extends IdentifierBridge<Object>> providedIdentifierBridge;
	private final IdentityMappingMode containedEntityIdentityMappingMode;
	private final TenancyMode tenancyMode;
	private final ReindexOnUpdate defaultReindexOnUpdate;

	private final FailureHandler failureHandler;
	private final ThreadPoolProvider threadPoolProvider;

	private final PojoMapperDelegate<MPBS> delegate;
	private final PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider;
	private final ContainerExtractorBinder extractorBinder;
	private final PojoMappingHelper mappingHelper;

	// Use a LinkedHashSet for deterministic iteration
	private final Set<PojoRawTypeModel<?>> entityTypes = new LinkedHashSet<>();
	private final Set<PojoRawTypeModel<?>> indexedEntityTypes = new LinkedHashSet<>();
	private final Set<PojoRawTypeModel<?>> initialMappedTypes = new LinkedHashSet<>();
	private final PojoTypeManagerContainer.Builder typeManagerContainerBuilder = PojoTypeManagerContainer.builder();
	private PojoSearchQueryElementRegistry searchQueryElementRegistry;

	private boolean closed = false;

	public PojoMapper(MappingBuildContext buildContext,
			TypeMetadataContributorProvider<PojoTypeMetadataContributor> contributorProvider,
			PojoBootstrapIntrospector introspector,
			ContainerExtractorBinder extractorBinder,
			BridgeResolver bridgeResolver,
			BeanReference<? extends IdentifierBridge<Object>> providedIdentifierBridge,
			IdentityMappingMode containedEntityIdentityMappingMode,
			TenancyMode tenancyMode, ReindexOnUpdate defaultReindexOnUpdate,
			PojoMapperDelegate<MPBS> delegate) {
		this.failureCollector = buildContext.failureCollector();
		this.contributorProvider = contributorProvider;
		this.containedEntityIdentityMappingMode = containedEntityIdentityMappingMode;
		this.tenancyMode = tenancyMode;
		this.defaultReindexOnUpdate = defaultReindexOnUpdate;

		this.failureHandler = buildContext.failureHandler();
		this.threadPoolProvider = buildContext.threadPoolProvider();

		this.delegate = delegate;

		this.providedIdentifierBridge = providedIdentifierBridge;

		typeAdditionalMetadataProvider = new PojoTypeAdditionalMetadataProvider(
				buildContext.beanResolver(), contributorProvider
		);

		this.extractorBinder = extractorBinder;

		PojoIndexModelBinder indexModelBinder = new PojoIndexModelBinder(
				buildContext, introspector, extractorBinder, bridgeResolver, typeAdditionalMetadataProvider
		);

		mappingHelper = new PojoMappingHelper( buildContext.beanResolver(), failureCollector, contributorProvider,
				introspector, typeAdditionalMetadataProvider, indexModelBinder );
	}

	@Override
	public void closeOnFailure() {
		if ( !closed ) {
			closed = true;
			try ( Closer<RuntimeException> closer = new Closer<>() ) {
				closer.push( PojoTypeManagerContainer.Builder::closeOnFailure, typeManagerContainerBuilder );
				closer.push( PojoMapperDelegate::closeOnFailure, delegate );
			}
		}
	}

	@Override
	public void prepareMappedTypes(BackendsInfo backendsInfo) {
		Collection<? extends MappableTypeModel> encounteredTypes = contributorProvider.typesContributedTo();
		for ( MappableTypeModel mappableTypeModel : encounteredTypes ) {
			try {
				if ( !( mappableTypeModel instanceof PojoRawTypeModel ) ) {
					throw new AssertionFailure(
							"Expected the mappable type model to be an instance of " + PojoRawTypeModel.class
									+ ", got " + mappableTypeModel + " instead."
					);
				}

				PojoRawTypeModel<?> rawTypeModel = (PojoRawTypeModel<?>) mappableTypeModel;
				prepareEntityOrIndexedType( rawTypeModel, backendsInfo );
				initialMappedTypes.add( rawTypeModel );
			}
			catch (RuntimeException e) {
				failureCollector.withContext( EventContexts.fromType( mappableTypeModel ) )
						.add( e );
			}
		}

		log.detectedMappedTypes( entityTypes, indexedEntityTypes, initialMappedTypes );

		// Register entity types and check for naming conflicts
		// we want to have a map of all entities by their name so that we can later use this information
		// when looking up supertypes by an entity name. E.g.
		// NotIndexedEntity(name=A); IndexedEntity(name=B) extends NotIndexedEntity
		// and let's say we want to do indexing plan filtering by "A" - this type is neither indexed nor contained
		// but we still need to be able to identify it.
		for ( PojoRawTypeModel<?> entityType : entityTypes ) {
			try {
				var metadata = typeAdditionalMetadataProvider.get( entityType )
						.getEntityTypeMetadata()
						// This should not be possible since this is only called for entity types
						.orElseThrow( () -> new AssertionFailure(
								"Missing metadata for entity type '" + entityType ) );
				typeManagerContainerBuilder.addEntity(
						entityType, metadata.getEntityName(), metadata.getSecondaryEntityName()
				);
			}
			catch (RuntimeException e) {
				failureCollector.withContext( EventContexts.fromType( entityType ) )
						.add( e );
			}
		}
	}

	private void prepareEntityOrIndexedType(PojoRawTypeModel<?> rawTypeModel, BackendsInfo backendsInfo) {
		PojoTypeAdditionalMetadata metadata = typeAdditionalMetadataProvider.get( rawTypeModel );

		if ( metadata.isEntity() ) {
			entityTypes.add( rawTypeModel );
		}

		Optional<PojoIndexedTypeAdditionalMetadata> indexedTypeMetadataOptional = metadata.getIndexedTypeMetadata();
		// Ignore abstract types: indexing will be handled for concrete subtypes.
		if ( !rawTypeModel.isAbstract() && indexedTypeMetadataOptional.isPresent() ) {
			if ( !metadata.getEntityTypeMetadata().isPresent() ) {
				throw log.missingEntityTypeMetadata( rawTypeModel );
			}
			PojoIndexedTypeAdditionalMetadata indexedTypeMetadata = indexedTypeMetadataOptional.get();
			backendsInfo.collect( indexedTypeMetadata.backendName(), tenancyMode );
			indexedEntityTypes.add( rawTypeModel );
		}
	}

	@Override
	public void mapTypes(MappedIndexManagerFactory indexManagerFactory) {
		for ( PojoRawTypeModel<?> indexedEntityType : indexedEntityTypes ) {
			try {
				mapIndexedType( indexedEntityType, indexManagerFactory );
			}
			catch (RuntimeException e) {
				failureCollector.withContext( EventContexts.fromType( indexedEntityType ) )
						.add( e );
			}
		}

		if ( !failureCollector.hasFailure() ) {
			mappingHelper.checkPathTrackers();
		}

		PojoSearchQueryElementRegistryBuilder searchQueryElementRegistryBuilder =
				new PojoSearchQueryElementRegistryBuilder( mappingHelper );
		try {
			for ( PojoRawTypeModel<?> type : initialMappedTypes ) {
				searchQueryElementRegistryBuilder.process( type );
			}
			searchQueryElementRegistry = searchQueryElementRegistryBuilder.build();
		}
		catch (RuntimeException e) {
			searchQueryElementRegistryBuilder.closeOnFailure();
		}

		if ( !failureCollector.hasFailure() ) {
			mappingHelper.checkPathTrackers();
		}
	}

	private <E> void mapIndexedType(PojoRawTypeModel<E> indexedEntityType,
			MappedIndexManagerFactory indexManagerFactory) {
		PojoTypeAdditionalMetadata metadata = typeAdditionalMetadataProvider.get( indexedEntityType );
		// This metadata is guaranteed to exist; see prepareEntityOrIndexedType()
		PojoEntityTypeAdditionalMetadata entityTypeMetadata = metadata.getEntityTypeMetadata().get();
		PojoIndexedTypeAdditionalMetadata indexedTypeMetadata = metadata.getIndexedTypeMetadata().get();
		String entityName = entityTypeMetadata.getEntityName();
		String indexName = indexedTypeMetadata.indexName().orElse( entityName );

		MappedIndexManagerBuilder indexManagerBuilder = indexManagerFactory.createMappedIndexManager( mappingHelper,
				delegate, indexedTypeMetadata.backendName(), indexName, entityName );

		Optional<RoutingBinder> routingBinderOptional = indexedTypeMetadata.routingBinder();
		BoundRoutingBridge<E> routingBridge = null;
		if ( routingBinderOptional.isPresent() ) {
			PojoBootstrapIntrospector introspector = mappingHelper.introspector();
			PojoModelTypeRootElement<E> pojoModelRootElement = new PojoModelTypeRootElement<>(
					BoundPojoModelPath.root( indexedEntityType ), introspector, typeAdditionalMetadataProvider );
			PojoRoutingIndexingDependencyConfigurationContextImpl<E> dependencyContext =
					new PojoRoutingIndexingDependencyConfigurationContextImpl<>( introspector, extractorBinder,
							typeAdditionalMetadataProvider, indexedEntityType );
			routingBridge = new RoutingBindingContextImpl<>( mappingHelper.beanResolver(),
					introspector, indexedEntityType, pojoModelRootElement, dependencyContext,
					indexedTypeMetadata.routingBinderParams() )
					.applyBinder( routingBinderOptional.get() );
		}

		var identityMappingCollector = new PojoRootIdentityMappingCollector<>(
				indexedEntityType,
				mappingHelper,
				Optional.of( indexManagerBuilder.rootBindingContext() ),
				providedIdentifierBridge
		);
		var indexingProcessorBuilder = new PojoIndexingProcessorOriginalTypeNodeBuilder<>(
				BoundPojoModelPath.root( indexedEntityType ),
				mappingHelper, indexManagerBuilder.rootBindingContext(),
				identityMappingCollector,
				Collections.emptyList()
		);
		var extendedMappingCollector = delegate.createIndexedTypeExtendedMappingCollector( indexedEntityType, entityName );
		typeManagerContainerBuilder.addIndexed( indexedEntityType,
				entityName, entityTypeMetadata.getSecondaryEntityName(), identityMappingCollector,
				extendedMappingCollector, routingBridge, indexingProcessorBuilder, indexManagerBuilder );
		collectIndexMapping( indexedEntityType, indexingProcessorBuilder );
	}

	@Override
	public MPBS prepareBuild() throws MappingAbortedException {
		PojoImplicitReindexingResolverBuildingHelper reindexingResolverBuildingHelper =
				new PojoImplicitReindexingResolverBuildingHelper(
						extractorBinder, typeAdditionalMetadataProvider, entityTypes,
						defaultReindexOnUpdate
				);

		PojoMappingDelegate mappingDelegate;
		try {
			// Build indexing processors, thereby collecting dependencies.
			// This must happen for all types before anything else,
			// because it has side-effects on other type managers (their reindexing resolvers in particular).
			for ( PojoIndexedTypeManager.Builder<?> builder : typeManagerContainerBuilder.indexed.values() ) {
				preBuildIndexingProcessorAndCollectDependencies( builder, reindexingResolverBuildingHelper );
			}
			if ( failureCollector.hasFailure() ) {
				throw new MappingAbortedException();
			}

			// Pre-build what can be for indexed types
			for ( PojoIndexedTypeManager.Builder<?> builder : typeManagerContainerBuilder.indexed.values() ) {
				try {
					preBuildIndexed( builder, reindexingResolverBuildingHelper );
				}
				catch (RuntimeException e) {
					failureCollector.withContext( PojoEventContexts.fromType( builder.typeModel ) )
							.add( e );
				}
			}

			// Pre-build what can be for contained types
			for ( PojoRawTypeModel<?> entityType : entityTypes ) {
				try {
					preBuildIfContained( entityType, reindexingResolverBuildingHelper );
				}
				catch (RuntimeException e) {
					failureCollector.withContext( PojoEventContexts.fromType( entityType ) )
							.add( e );
				}
			}

			if ( failureCollector.hasFailure() ) {
				throw new MappingAbortedException();
			}

			// Build the type managers
			var typeManagerContainer = typeManagerContainerBuilder.build( reindexingResolverBuildingHelper );

			mappingDelegate = new PojoMappingDelegateImpl(
					threadPoolProvider, failureHandler, tenancyMode,
					typeManagerContainer,
					searchQueryElementRegistry
			);
		}
		catch (MappingAbortedException | RuntimeException e) {
			new SuppressingCloser( e )
					.push(
							PojoSearchQueryElementRegistry::close,
							searchQueryElementRegistry
					)
					.push(
							PojoImplicitReindexingResolverBuildingHelper::closeOnFailure,
							reindexingResolverBuildingHelper
					)
					.push(
							PojoTypeManagerContainer.Builder::closeOnFailure,
							typeManagerContainerBuilder
					)
					.push( PojoMapperDelegate::closeOnFailure, delegate );
			throw e;
		}
		closed = true;

		try {
			return delegate.prepareBuild( mappingDelegate );
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( PojoMapperDelegate::closeOnFailure, delegate )
					.push( mappingDelegate );
			throw e;
		}
	}

	private <T> void collectIndexMapping(PojoRawTypeModel<T> type, PojoIndexMappingCollectorTypeNode collector) {
		for ( PojoTypeMetadataContributor contributor : contributorProvider.get( type ) ) {
			contributor.contributeIndexMapping( collector );
		}
	}

	private <E> void preBuildIndexingProcessorAndCollectDependencies(PojoIndexedTypeManager.Builder<E> builder,
			PojoImplicitReindexingResolverBuildingHelper helper) {
		var dependencyCollector = helper.createDependencyCollector( builder.typeModel );

		if ( builder.routingBridge != null ) {
			builder.routingBridge.contributeDependencies( dependencyCollector );
		}

		builder.preBuildIndexingProcessor( dependencyCollector );
	}

	private <E> void preBuildIndexed(PojoIndexedTypeManager.Builder<E> builder,
			PojoImplicitReindexingResolverBuildingHelper helper) {
		builder.preBuildIdentifierMapping( IdentityMappingMode.REQUIRED );
		builder.reindexingResolver( helper.build( builder.typeModel ) );
		preBuildOtherMetadata( builder, helper );
		builder.preBuildIndexManager();
	}

	private <E> void preBuildOtherMetadata(AbstractPojoTypeManager.Builder<E> builder,
			PojoImplicitReindexingResolverBuildingHelper helper) {
		PojoRawTypeModel<E> typeModel = builder.typeModel;
		var loadingBinderRefOptional = typeModel.ascendingSuperTypes()
				.map( superType -> typeAdditionalMetadataProvider.get( superType )
						.getEntityTypeMetadata()
						.map( PojoEntityTypeAdditionalMetadata::getLoadingBinderRef )
						.orElse( null ) )
				.filter( Objects::nonNull )
				.findFirst();
		builder.preBuildOtherMetadata( mappingHelper.beanResolver(), mappingHelper.introspector(),
				helper.isSingleConcreteTypeInEntityHierarchy( typeModel ),
				helper.runtimePathsBuildingHelper( typeModel ).pathOrdinals(),
				loadingBinderRefOptional );
	}

	private <E> void preBuildIfContained(PojoRawTypeModel<E> entityType,
			PojoImplicitReindexingResolverBuildingHelper reindexingResolverBuildingHelper) {
		// Ignore abstract classes: we create one manager per concrete subclass, which is enough.
		if ( entityType.isAbstract()
				// Ignore indexed types: those are already taken care of elsewhere.
				|| typeManagerContainerBuilder.indexed.containsKey( entityType ) ) {
			return;
		}

		PojoEntityTypeAdditionalMetadata entityTypeMetadata = typeAdditionalMetadataProvider.get( entityType )
				.getEntityTypeMetadata()
				// This should not be possible since this method is only called for entity types (see caller)
				.orElseThrow( () -> new AssertionFailure( "Missing metadata for entity type '" + entityType ) );
		Optional<? extends PojoImplicitReindexingResolver<E>> reindexingResolverOptional =
				reindexingResolverBuildingHelper.buildOptional( entityType );
		// Ignore types that are not actually contained.
		if ( reindexingResolverOptional.isEmpty() ) {
			return;
		}

		String entityName = entityTypeMetadata.getEntityName();
		var extendedMappingCollector = delegate.createContainedTypeExtendedMappingCollector( entityType, entityName );
		var identityMappingCollector = new PojoRootIdentityMappingCollector<>( entityType, mappingHelper,
				Optional.empty(), providedIdentifierBridge );
		var builder = typeManagerContainerBuilder.addContained( entityType,
				entityName, entityTypeMetadata.getSecondaryEntityName(), identityMappingCollector,
				extendedMappingCollector );

		collectIndexMapping( entityType, identityMappingCollector.toMappingCollectorRootNode() );
		builder.preBuildIdentifierMapping( containedEntityIdentityMappingMode );
		builder.reindexingResolver( reindexingResolverOptional.get() );
		preBuildOtherMetadata( builder, reindexingResolverBuildingHelper );
	}

}
