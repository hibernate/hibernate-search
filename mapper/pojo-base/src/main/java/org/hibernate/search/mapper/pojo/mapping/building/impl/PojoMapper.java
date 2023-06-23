/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
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
import org.hibernate.search.mapper.pojo.identity.impl.IdentifierMappingImplementor;
import org.hibernate.search.mapper.pojo.identity.impl.IdentityMappingMode;
import org.hibernate.search.mapper.pojo.identity.impl.PojoRootIdentityMappingCollector;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoContainedTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMapperDelegate;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoContainedTypeManager;
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
	// Use a LinkedHashMap for deterministic iteration
	private final Map<PojoRawTypeModel<?>, PojoIndexedTypeManagerBuilder<?>> indexedTypeManagerBuilders =
			new LinkedHashMap<>();
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
				closer.pushAll( PojoIndexedTypeManagerBuilder::closeOnFailure, indexedTypeManagerBuilders.values() );
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

		PojoIndexedTypeManagerBuilder<E> builder = new PojoIndexedTypeManagerBuilder<>( entityName, indexedEntityType,
				mappingHelper, indexManagerBuilder,
				delegate.createIndexedTypeExtendedMappingCollector( indexedEntityType, entityName ),
				providedIdentifierBridge, routingBridge );

		// Put the builder in the map before anything else, so it will be closed on error
		indexedTypeManagerBuilders.put( indexedEntityType, builder );

		collectIndexMapping( indexedEntityType, builder.asCollector() );
	}

	@Override
	public MPBS prepareBuild() throws MappingAbortedException {
		PojoTypeManagerContainer.Builder typeManagerContainerBuilder = PojoTypeManagerContainer.builder();
		PojoImplicitReindexingResolverBuildingHelper reindexingResolverBuildingHelper =
				new PojoImplicitReindexingResolverBuildingHelper(
						extractorBinder, typeAdditionalMetadataProvider, entityTypes,
						defaultReindexOnUpdate
				);

		PojoMappingDelegate mappingDelegate;
		try {
			// First step: build the processors and contribute to the reindexing resolvers
			for ( PojoIndexedTypeManagerBuilder<?> pojoIndexedTypeManagerBuilder : indexedTypeManagerBuilders.values() ) {
				pojoIndexedTypeManagerBuilder.preBuild( reindexingResolverBuildingHelper );
			}
			if ( failureCollector.hasFailure() ) {
				throw new MappingAbortedException();
			}

			// Second step: build the indexed type managers and their reindexing resolvers
			for ( Map.Entry<PojoRawTypeModel<?>, PojoIndexedTypeManagerBuilder<?>> entry : indexedTypeManagerBuilders
					.entrySet() ) {
				PojoRawTypeModel<?> typeModel = entry.getKey();
				PojoIndexedTypeManagerBuilder<?> pojoIndexedTypeManagerBuilder = entry.getValue();
				try {
					pojoIndexedTypeManagerBuilder.buildAndAddTo(
							typeManagerContainerBuilder, reindexingResolverBuildingHelper
					);
				}
				catch (RuntimeException e) {
					failureCollector.withContext( PojoEventContexts.fromType( typeModel ) )
							.add( e );
				}
			}
			// Third step: build the non-indexed, contained type managers and their reindexing resolvers
			for ( PojoRawTypeModel<?> entityType : entityTypes ) {
				// Ignore abstract classes: we create one manager per concrete subclass, which is enough
				if ( !entityType.isAbstract() && !indexedTypeManagerBuilders.containsKey( entityType ) ) {
					try {
						buildAndAddContainedTypeManagerTo(
								typeManagerContainerBuilder, reindexingResolverBuildingHelper, entityType
						);
					}
					catch (RuntimeException e) {
						failureCollector.withContext( PojoEventContexts.fromType( entityType ) )
								.add( e );
					}
				}
				// we want to have a map of all entities by their name so that we can later use this information
				// when looking up supertypes by an entity name. E.g.
				// NotIndexedEntity(name=A); IndexedEntity(name=B) extends NotIndexedEntity
				// and let's say we want to do indexing plan filtering by "A" - this type is neither indexed nor contained
				// but we still need to be able to identify it.
				typeManagerContainerBuilder.addEntity(
						typeAdditionalMetadataProvider.get( entityType )
								.getEntityTypeMetadata()
								// This should not be possible since this method is only called for entity types (see caller)
								.orElseThrow( () -> new AssertionFailure(
										"Missing metadata for entity type '" + entityType ) )
								.getEntityName(),
						entityType
				);
			}
			if ( failureCollector.hasFailure() ) {
				throw new MappingAbortedException();
			}

			mappingDelegate = new PojoMappingDelegateImpl(
					threadPoolProvider, failureHandler, tenancyMode,
					typeManagerContainerBuilder.build(),
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

	private <T> void buildAndAddContainedTypeManagerTo(
			PojoTypeManagerContainer.Builder typeManagerContainerBuilder,
			PojoImplicitReindexingResolverBuildingHelper reindexingResolverBuildingHelper,
			PojoRawTypeModel<T> entityType) {
		PojoEntityTypeAdditionalMetadata entityTypeMetadata = typeAdditionalMetadataProvider.get( entityType )
				.getEntityTypeMetadata()
				// This should not be possible since this method is only called for entity types (see caller)
				.orElseThrow( () -> new AssertionFailure( "Missing metadata for entity type '" + entityType ) );
		Optional<? extends PojoImplicitReindexingResolver<T>> reindexingResolverOptional =
				reindexingResolverBuildingHelper.buildOptional( entityType );
		if ( reindexingResolverOptional.isPresent() ) {
			String entityName = entityTypeMetadata.getEntityName();

			PojoImplicitReindexingResolver<T> reindexingResolver = reindexingResolverOptional.get();

			PojoContainedTypeExtendedMappingCollector extendedMappingCollector = delegate
					.createContainedTypeExtendedMappingCollector( entityType, entityName );

			extendedMappingCollector.dirtyFilter( reindexingResolver.dirtySelfOrContainingFilter() );
			extendedMappingCollector.dirtyContainingAssociationFilter(
					reindexingResolver.associationInverseSideResolver().dirtyContainingAssociationFilter() );

			PojoRootIdentityMappingCollector<T> identityMappingCollector = new PojoRootIdentityMappingCollector<>(
					entityType, mappingHelper, Optional.empty(), providedIdentifierBridge
			);
			collectIndexMapping( entityType, identityMappingCollector.toMappingCollectorRootNode() );
			IdentifierMappingImplementor<?, T> identifierMapping = identityMappingCollector
					.buildAndContributeTo( extendedMappingCollector, containedEntityIdentityMappingMode );

			PojoContainedTypeManager<?, T> typeManager = new PojoContainedTypeManager<>(
					entityName, entityType.typeIdentifier(), entityType.caster(),
					reindexingResolverBuildingHelper.isSingleConcreteTypeInEntityHierarchy( entityType ),
					identifierMapping,
					reindexingResolverBuildingHelper.runtimePathsBuildingHelper( entityType ).pathOrdinals(),
					reindexingResolver
			);
			log.containedTypeManager( entityType, typeManager );
			typeManagerContainerBuilder.addContained( entityType, typeManager );
		}
	}

	private <T> void collectIndexMapping(PojoRawTypeModel<T> type, PojoIndexMappingCollectorTypeNode collector) {
		for ( PojoTypeMetadataContributor contributor : contributorProvider.get( type ) ) {
			contributor.contributeIndexMapping( collector );
		}
	}

}
