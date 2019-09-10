/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexManagerBuildingState;
import org.hibernate.search.engine.mapper.mapping.building.spi.Mapper;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingAbortedException;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.engine.mapper.mapping.spi.MappingBuildContext;
import org.hibernate.search.engine.mapper.mapping.spi.MappingPartialBuildState;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.BridgeResolver;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoAssociationPathInverter;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoImplicitReindexingResolverBuildingHelper;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorBinder;
import org.hibernate.search.mapper.pojo.extractor.spi.ContainerExtractorRegistry;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoContainedTypeManager;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoContainedTypeManagerContainer;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoIndexedTypeManagerContainer;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoMappingDelegateImpl;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMapperDelegate;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl.PojoTypeAdditionalMetadataProvider;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilterFactory;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.typepattern.impl.TypePatternMatcherFactory;
import org.hibernate.search.mapper.pojo.reporting.impl.PojoEventContexts;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class PojoMapper<MPBS extends MappingPartialBuildState> implements Mapper<MPBS> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ContextualFailureCollector failureCollector;
	private final TypeMetadataContributorProvider<PojoTypeMetadataContributor> contributorProvider;
	private final boolean implicitProvidedId;
	private final PojoMapperDelegate<MPBS> delegate;
	private final PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider;
	private final ContainerExtractorBinder extractorBinder;
	private final PojoMappingHelper mappingHelper;

	// Use a LinkedHashMap for deterministic iteration
	private final Map<PojoRawTypeModel<?>,PojoIndexedTypeManagerBuilder<?, ?>> indexedTypeManagerBuilders =
			new LinkedHashMap<>();

	private boolean closed = false;

	public PojoMapper(MappingBuildContext buildContext,
			TypeMetadataContributorProvider<PojoTypeMetadataContributor> contributorProvider,
			PojoBootstrapIntrospector introspector,
			ContainerExtractorRegistry containerExtractorRegistry,
			boolean implicitProvidedId,
			PojoMapperDelegate<MPBS> delegate) {
		this.failureCollector = buildContext.getFailureCollector();
		this.contributorProvider = contributorProvider;
		this.implicitProvidedId = implicitProvidedId;
		this.delegate = delegate;

		typeAdditionalMetadataProvider = new PojoTypeAdditionalMetadataProvider(
				buildContext.getBeanResolver(), failureCollector, contributorProvider
		);

		TypePatternMatcherFactory typePatternMatcherFactory = new TypePatternMatcherFactory( introspector );
		extractorBinder = new ContainerExtractorBinder( buildContext, containerExtractorRegistry, typePatternMatcherFactory );

		BridgeResolver bridgeResolver = new BridgeResolver( typePatternMatcherFactory );
		PojoIndexModelBinder indexModelBinder = new PojoIndexModelBinderImpl(
				buildContext, introspector, extractorBinder, bridgeResolver, typeAdditionalMetadataProvider
		);

		mappingHelper = new PojoMappingHelper(
				failureCollector, contributorProvider, indexModelBinder
		);
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
	public void addIndexed(MappableTypeModel typeModel, IndexManagerBuildingState<?> indexManagerBuildingState) {
		if ( !( typeModel instanceof PojoRawTypeModel ) ) {
			throw new AssertionFailure(
					"Expected the indexed type model to be an instance of " + PojoRawTypeModel.class
					+ ", got " + typeModel + " instead. There is probably a bug in the mapper implementation"
			);
		}

		PojoRawTypeModel<?> entityTypeModel = (PojoRawTypeModel<?>) typeModel;
		PojoIndexedTypeManagerBuilder<?, ?> builder = createIndexedTypeManagerBuilder(
				entityTypeModel, indexManagerBuildingState
		);
		// Put the builder in the map before anything else, so it will be closed on error
		indexedTypeManagerBuilders.put( entityTypeModel, builder );

		PojoMappingCollectorTypeNode collector = builder.asCollector();

		contributorProvider.get( entityTypeModel )
				.forEach( c -> c.contributeMapping( collector ) );
	}

	@Override
	public MPBS prepareBuild() throws MappingAbortedException {
		Set<PojoRawTypeModel<?>> entityTypes = computeEntityTypes();
		log.detectedEntityTypes( entityTypes );

		PojoIndexedTypeManagerContainer.Builder indexedTypeManagerContainerBuilder =
				PojoIndexedTypeManagerContainer.builder();
		PojoContainedTypeManagerContainer.Builder containedTypeManagerContainerBuilder =
				PojoContainedTypeManagerContainer.builder();
		PojoAssociationPathInverter pathInverter = new PojoAssociationPathInverter(
				typeAdditionalMetadataProvider, extractorBinder
		);
		PojoImplicitReindexingResolverBuildingHelper reindexingResolverBuildingHelper =
				new PojoImplicitReindexingResolverBuildingHelper(
						extractorBinder, typeAdditionalMetadataProvider, pathInverter, entityTypes
				);

		PojoMappingDelegate mappingDelegate;
		try {
			// First step: build the processors and contribute to the reindexing resolvers
			for ( PojoIndexedTypeManagerBuilder<?, ?> pojoIndexedTypeManagerBuilder : indexedTypeManagerBuilders.values() ) {
				pojoIndexedTypeManagerBuilder.preBuild( reindexingResolverBuildingHelper );
			}
			if ( failureCollector.hasFailure() ) {
				throw new MappingAbortedException();
			}

			// Second step: build the indexed type managers and their reindexing resolvers
			for ( Map.Entry<PojoRawTypeModel<?>, PojoIndexedTypeManagerBuilder<?, ?>> entry
					: indexedTypeManagerBuilders.entrySet() ) {
				PojoRawTypeModel<?> typeModel = entry.getKey();
				PojoIndexedTypeManagerBuilder<?, ?> pojoIndexedTypeManagerBuilder = entry.getValue();
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

	private <T> void buildAndAddContainedTypeManagerTo(
			PojoContainedTypeManagerContainer.Builder containedTypeManagerContainerBuilder,
			PojoImplicitReindexingResolverBuildingHelper reindexingResolverBuildingHelper,
			PojoRawTypeModel<T> entityType) {
		/*
		 * TODO offer more flexibility to mapper implementations, allowing them to define their own dirtiness state?
		 * Note this will require to allow them to define their own work plan APIs.
		 */
		PojoPathFilterFactory<Set<String>> pathFilterFactory = typeAdditionalMetadataProvider.get( entityType )
				.getEntityTypeMetadata().orElseThrow( () -> log.missingEntityTypeMetadata( entityType ) )
				.getPathFilterFactory();
		Optional<? extends PojoImplicitReindexingResolver<T, Set<String>>> reindexingResolverOptional =
				reindexingResolverBuildingHelper.build( entityType, pathFilterFactory );
		if ( reindexingResolverOptional.isPresent() ) {
			// Nothing to contribute to contained types at the moment,
			// but create the collector just so the mapper knows the type is contained
			delegate.createContainedTypeExtendedMappingCollector( entityType );

			PojoContainedTypeManager<T> typeManager = new PojoContainedTypeManager<>(
					entityType.getJavaClass(), entityType.getCaster(),
					reindexingResolverOptional.get()
			);
			log.createdPojoContainedTypeManager( typeManager );
			containedTypeManagerContainerBuilder.add( entityType, typeManager );
		}
	}

	private Set<PojoRawTypeModel<?>> computeEntityTypes() {
		// Use a LinkedHashSet for deterministic iteration
		Set<PojoRawTypeModel<?>> entityTypes = new LinkedHashSet<>();
		Collection<? extends MappableTypeModel> encounteredTypes = contributorProvider.getTypesContributedTo();
		for ( MappableTypeModel mappableTypeModel : encounteredTypes ) {
			PojoRawTypeModel<?> pojoRawTypeModel = (PojoRawTypeModel<?>) mappableTypeModel;
			if ( typeAdditionalMetadataProvider.get( pojoRawTypeModel ).isEntity() ) {
				entityTypes.add( pojoRawTypeModel );
			}
		}
		return Collections.unmodifiableSet( entityTypes );
	}

	private <E, D extends DocumentElement> PojoIndexedTypeManagerBuilder<E, D> createIndexedTypeManagerBuilder(
			PojoRawTypeModel<E> entityTypeModel, IndexManagerBuildingState<D> indexManagerBuildingState) {
		return new PojoIndexedTypeManagerBuilder<>(
				entityTypeModel,
				typeAdditionalMetadataProvider.get( entityTypeModel ),
				mappingHelper,
				indexManagerBuildingState,
				delegate.createIndexedTypeExtendedMappingCollector(
						entityTypeModel, indexManagerBuildingState.getIndexName()
				),
				implicitProvidedId
		);
	}

}
