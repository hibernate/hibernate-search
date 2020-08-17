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
import java.util.function.Consumer;

import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappedIndexManagerBuilder;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEmbeddedDefinition;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEmbeddedPathTracker;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingMapperContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappedIndexManagerFactory;
import org.hibernate.search.engine.mapper.mapping.building.spi.Mapper;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingAbortedException;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingBuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingPartialBuildState;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.BoundRoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.RoutingBindingContextImpl;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.BridgeResolver;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoImplicitReindexingResolverBuildingHelper;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingBinder;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorBinder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMapperDelegate;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoContainedTypeManager;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoContainedTypeManagerContainer;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoIndexedTypeManagerContainer;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoMappingDelegateImpl;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl.PojoTypeAdditionalMetadataProvider;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoEntityTypeAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoIndexedTypeAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoTypeAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.dependency.impl.PojoRoutingIndexingDependencyConfigurationContextImpl;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelTypeRootElement;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilterFactory;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.reporting.impl.PojoEventContexts;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class PojoMapper<MPBS extends MappingPartialBuildState> implements Mapper<MPBS>,
		IndexedEntityBindingMapperContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ContextualFailureCollector failureCollector;
	private final TypeMetadataContributorProvider<PojoTypeMetadataContributor> contributorProvider;
	private final BeanReference<? extends IdentifierBridge<Object>> providedIdentifierBridge;
	private final boolean multiTenancyEnabled;
	private final ReindexOnUpdate defaultReindexOnUpdate;

	private final FailureHandler failureHandler;
	private final ThreadPoolProvider threadPoolProvider;
	private final TimingSource timingSource;

	private final PojoMapperDelegate<MPBS> delegate;
	private final PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider;
	private final ContainerExtractorBinder extractorBinder;
	private final PojoMappingHelper mappingHelper;

	// Use a LinkedHashSet for deterministic iteration
	private final Set<PojoRawTypeModel<?>> entityTypes = new LinkedHashSet<>();
	private final Set<PojoRawTypeModel<?>> indexedEntityTypes = new LinkedHashSet<>();
	// Use a LinkedHashMap for deterministic iteration
	private final Map<PojoRawTypeModel<?>,PojoIndexedTypeManagerBuilder<?>> indexedTypeManagerBuilders =
			new LinkedHashMap<>();
	// Use a LinkedHashMap for deterministic iteration
	private final Map<IndexedEmbeddedDefinition, IndexedEmbeddedPathTracker> pathTrackers = new LinkedHashMap<>();

	private boolean closed = false;

	public PojoMapper(MappingBuildContext buildContext,
			TypeMetadataContributorProvider<PojoTypeMetadataContributor> contributorProvider,
			PojoBootstrapIntrospector introspector,
			ContainerExtractorBinder extractorBinder,
			BridgeResolver bridgeResolver,
			BeanReference<? extends IdentifierBridge<Object>> providedIdentifierBridge,
			boolean multiTenancyEnabled, ReindexOnUpdate defaultReindexOnUpdate,
			PojoMapperDelegate<MPBS> delegate) {
		this.failureCollector = buildContext.failureCollector();
		this.contributorProvider = contributorProvider;
		this.multiTenancyEnabled = multiTenancyEnabled;
		this.defaultReindexOnUpdate = defaultReindexOnUpdate;

		this.failureHandler = buildContext.failureHandler();
		this.threadPoolProvider = buildContext.threadPoolProvider();
		this.timingSource = buildContext.timingSource();

		this.delegate = delegate;

		this.providedIdentifierBridge = providedIdentifierBridge;

		typeAdditionalMetadataProvider = new PojoTypeAdditionalMetadataProvider(
				buildContext.beanResolver(), failureCollector, contributorProvider
		);

		this.extractorBinder = extractorBinder;

		PojoIndexModelBinder indexModelBinder = new PojoIndexModelBinderImpl(
				buildContext, introspector, extractorBinder, bridgeResolver, typeAdditionalMetadataProvider
		);

		mappingHelper = new PojoMappingHelper( buildContext.beanResolver(), failureCollector, contributorProvider,
				introspector, indexModelBinder );
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
	public void prepareIndexedTypes(Consumer<Optional<String>> backendNameCollector) {
		Collection<? extends MappableTypeModel> encounteredTypes = contributorProvider.typesContributedTo();
		for ( MappableTypeModel mappableTypeModel : encounteredTypes ) {
			try {
				if ( !( mappableTypeModel instanceof PojoRawTypeModel ) ) {
					throw new AssertionFailure(
							"Expected the mappable type model to be an instance of " + PojoRawTypeModel.class
									+ ", got " + mappableTypeModel + " instead. There is probably a bug in the mapper implementation"
					);
				}

				PojoRawTypeModel<?> rawTypeModel = (PojoRawTypeModel<?>) mappableTypeModel;
				prepareEntityOrIndexedType( rawTypeModel, backendNameCollector );
			}
			catch (RuntimeException e) {
				failureCollector.withContext( EventContexts.fromType( mappableTypeModel ) )
						.add( e );
			}
		}

		log.detectedEntityTypes( entityTypes, indexedEntityTypes );
	}

	private void prepareEntityOrIndexedType(PojoRawTypeModel<?> rawTypeModel,
			Consumer<Optional<String>> backendNameCollector) {
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
			backendNameCollector.accept( indexedTypeMetadata.backendName() );
			indexedEntityTypes.add( rawTypeModel );
		}
	}

	@Override
	public void mapIndexedTypes(MappedIndexManagerFactory indexManagerFactory) {
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
			checkPathTrackers();
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

		MappedIndexManagerBuilder indexManagerBuilder = indexManagerFactory.createMappedIndexManager( this,
				indexedTypeMetadata.backendName(), indexName, entityName, multiTenancyEnabled );

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
					introspector, indexedEntityType, pojoModelRootElement, dependencyContext )
					.applyBinder( routingBinderOptional.get() );
		}

		PojoIndexedTypeManagerBuilder<E> builder = new PojoIndexedTypeManagerBuilder<>( indexedEntityType,
				entityTypeMetadata, mappingHelper, indexManagerBuilder,
				delegate.createIndexedTypeExtendedMappingCollector( indexedEntityType, entityName, indexName ),
				providedIdentifierBridge, routingBridge, mappingHelper.beanResolver() );

		// Put the builder in the map before anything else, so it will be closed on error
		indexedTypeManagerBuilders.put( indexedEntityType, builder );

		PojoMappingCollectorTypeNode collector = builder.asCollector();

		contributorProvider.get( indexedEntityType )
				.forEach( c -> c.contributeMapping( collector ) );
	}

	@Override
	public MPBS prepareBuild() throws MappingAbortedException {
		PojoIndexedTypeManagerContainer.Builder indexedTypeManagerContainerBuilder =
				PojoIndexedTypeManagerContainer.builder();
		PojoContainedTypeManagerContainer.Builder containedTypeManagerContainerBuilder =
				PojoContainedTypeManagerContainer.builder();
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
			for ( Map.Entry<PojoRawTypeModel<?>, PojoIndexedTypeManagerBuilder<?>> entry
					: indexedTypeManagerBuilders.entrySet() ) {
				PojoRawTypeModel<?> typeModel = entry.getKey();
				PojoIndexedTypeManagerBuilder<?> pojoIndexedTypeManagerBuilder = entry.getValue();
				try {
					pojoIndexedTypeManagerBuilder.buildAndAddTo(
							indexedTypeManagerContainerBuilder, reindexingResolverBuildingHelper,
							typeAdditionalMetadataProvider.get( typeModel )
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
								containedTypeManagerContainerBuilder, reindexingResolverBuildingHelper, entityType
						);
					}
					catch (RuntimeException e) {
						failureCollector.withContext( PojoEventContexts.fromType( entityType ) )
								.add( e );
					}
				}
			}
			if ( failureCollector.hasFailure() ) {
				throw new MappingAbortedException();
			}

			mappingDelegate = new PojoMappingDelegateImpl(
					threadPoolProvider, failureHandler, timingSource,
					indexedTypeManagerContainerBuilder.build(),
					containedTypeManagerContainerBuilder.build()
			);
		}
		catch (MappingAbortedException | RuntimeException e) {
			new SuppressingCloser( e )
					.push(
							PojoImplicitReindexingResolverBuildingHelper::closeOnFailure,
							reindexingResolverBuildingHelper
					)
					.push(
							PojoIndexedTypeManagerContainer.Builder::closeOnFailure,
							indexedTypeManagerContainerBuilder
					)
					.push(
							PojoContainedTypeManagerContainer.Builder::closeOnFailure,
							containedTypeManagerContainerBuilder
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

	@Override
	public IndexedEmbeddedPathTracker getOrCreatePathTracker(IndexedEmbeddedDefinition definition) {
		return pathTrackers.computeIfAbsent( definition, IndexedEmbeddedPathTracker::new );
	}

	private void checkPathTrackers() {
		for ( Map.Entry<IndexedEmbeddedDefinition, IndexedEmbeddedPathTracker> entry : pathTrackers.entrySet() ) {
			IndexedEmbeddedPathTracker pathTracker = entry.getValue();
			Set<String> uselessIncludePaths = pathTracker.uselessIncludePaths();
			if ( !uselessIncludePaths.isEmpty() ) {
				Set<String> encounteredFieldPaths = pathTracker.encounteredFieldPaths();
				failureCollector.add( log.uselessIncludePathFilters(
						uselessIncludePaths, encounteredFieldPaths,
						EventContexts.fromType( entry.getKey().definingTypeModel() )
				) );
			}
		}
	}

	private <T> void buildAndAddContainedTypeManagerTo(
			PojoContainedTypeManagerContainer.Builder containedTypeManagerContainerBuilder,
			PojoImplicitReindexingResolverBuildingHelper reindexingResolverBuildingHelper,
			PojoRawTypeModel<T> entityType) {
		/*
		 * TODO offer more flexibility to mapper implementations, allowing them to define their own dirtiness state?
		 * Note this will require to allow them to define their own indexing plan APIs.
		 */
		PojoEntityTypeAdditionalMetadata entityTypeMetadata = typeAdditionalMetadataProvider.get( entityType )
				.getEntityTypeMetadata().orElseThrow( () -> log.missingEntityTypeMetadata( entityType ) );
		PojoPathFilterFactory<Set<String>> pathFilterFactory = entityTypeMetadata.getPathFilterFactory();
		Optional<? extends PojoImplicitReindexingResolver<T, Set<String>>> reindexingResolverOptional =
				reindexingResolverBuildingHelper.buildOptional( entityType, pathFilterFactory );
		if ( reindexingResolverOptional.isPresent() ) {
			// Nothing to contribute to contained types at the moment,
			// but create the collector just so the mapper knows the type is contained
			delegate.createContainedTypeExtendedMappingCollector( entityType, entityTypeMetadata.getEntityName() );

			PojoContainedTypeManager<T> typeManager = new PojoContainedTypeManager<>(
					entityType.typeIdentifier(), entityType.caster(),
					reindexingResolverOptional.get()
			);
			log.createdPojoContainedTypeManager( typeManager );
			containedTypeManagerContainerBuilder.add( entityType, typeManager );
		}
	}

}
