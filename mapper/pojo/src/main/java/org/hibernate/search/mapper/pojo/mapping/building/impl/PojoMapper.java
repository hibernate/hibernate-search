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
import java.util.function.BiFunction;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingAbortedException;
import org.hibernate.search.engine.mapper.mapping.spi.MappingBuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexManagerBuildingState;
import org.hibernate.search.engine.mapper.mapping.building.spi.Mapper;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.mapper.pojo.bridge.impl.BridgeResolver;
import org.hibernate.search.mapper.pojo.dirtiness.building.impl.PojoAssociationPathInverter;
import org.hibernate.search.mapper.pojo.dirtiness.building.impl.PojoImplicitReindexingResolverBuildingHelper;
import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerValueExtractorBinder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.logging.spi.PojoEventContexts;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoContainedTypeManager;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoContainedTypeManagerContainer;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoIndexedTypeManagerContainer;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoMappingDelegateImpl;
import org.hibernate.search.mapper.pojo.mapping.impl.ProvidedStringIdentifierMapping;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl.PojoTypeAdditionalMetadataProvider;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilterFactory;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.typepattern.impl.TypePatternMatcherFactory;
import org.hibernate.search.engine.logging.spi.ContextualFailureCollector;
import org.hibernate.search.util.AssertionFailure;
import org.hibernate.search.util.impl.common.Closer;
import org.hibernate.search.util.impl.common.LoggerFactory;
import org.hibernate.search.util.impl.common.SuppressingCloser;

public class PojoMapper<M> implements Mapper<M> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ContextualFailureCollector failureCollector;
	private final ConfigurationPropertySource propertySource;
	private final TypeMetadataContributorProvider<PojoTypeMetadataContributor> contributorProvider;
	private final boolean implicitProvidedId;
	private final BiFunction<ConfigurationPropertySource, PojoMappingDelegate, MappingImplementor<M>> wrapperFactory;
	private final PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider;
	private final ContainerValueExtractorBinder extractorBinder;
	private final PojoMappingHelper mappingHelper;

	// Use a LinkedHashMap for deterministic iteration
	private final Map<PojoRawTypeModel<?>,PojoIndexedTypeManagerBuilder<?, ?>> indexedTypeManagerBuilders =
			new LinkedHashMap<>();

	private boolean closed = false;

	public PojoMapper(MappingBuildContext buildContext, ConfigurationPropertySource propertySource,
			TypeMetadataContributorProvider<PojoTypeMetadataContributor> contributorProvider,
			PojoBootstrapIntrospector introspector,
			boolean implicitProvidedId,
			BiFunction<ConfigurationPropertySource, PojoMappingDelegate, MappingImplementor<M>> wrapperFactory) {
		this.failureCollector = buildContext.getFailureCollector();
		this.propertySource = propertySource;
		this.contributorProvider = contributorProvider;
		this.implicitProvidedId = implicitProvidedId;
		this.wrapperFactory = wrapperFactory;

		typeAdditionalMetadataProvider = new PojoTypeAdditionalMetadataProvider(
				failureCollector, contributorProvider
		);

		TypePatternMatcherFactory typePatternMatcherFactory = new TypePatternMatcherFactory( introspector );
		extractorBinder = new ContainerValueExtractorBinder( buildContext, typePatternMatcherFactory );

		BridgeResolver bridgeResolver = new BridgeResolver( typePatternMatcherFactory );
		PojoIndexModelBinder indexModelBinder = new PojoIndexModelBinderImpl(
				buildContext, extractorBinder, bridgeResolver, typeAdditionalMetadataProvider
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
		PojoIndexedTypeManagerBuilder<?, ?> builder = new PojoIndexedTypeManagerBuilder<>(
				entityTypeModel,
				typeAdditionalMetadataProvider.get( entityTypeModel ),
				mappingHelper,
				indexManagerBuildingState,
				implicitProvidedId ? ProvidedStringIdentifierMapping.get() : null
		);
		// Put the builder in the map before anything else, so it will be closed on error
		indexedTypeManagerBuilders.put( entityTypeModel, builder );

		PojoMappingCollectorTypeNode collector = builder.asCollector();
		contributorProvider.forEach(
				entityTypeModel,
				c -> c.contributeMapping( collector )
		);
	}

	@Override
	public MappingImplementor<M> build() throws MappingAbortedException {
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

		// First phase: build the processors and contribute to the reindexing resolvers
		for ( PojoIndexedTypeManagerBuilder<?, ?> pojoIndexedTypeManagerBuilder : indexedTypeManagerBuilders.values() ) {
			pojoIndexedTypeManagerBuilder.preBuild( reindexingResolverBuildingHelper );
		}
		if ( failureCollector.hasFailure() ) {
			throw new MappingAbortedException();
		}

		PojoMappingDelegate mappingImplementor = null;
		try {
			// Second phase: build the indexed type managers and their reindexing resolvers
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
			// Third phase: build the non-indexed, contained type managers and their reindexing resolvers
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

			mappingImplementor = new PojoMappingDelegateImpl(
					indexedTypeManagerContainerBuilder.build(),
					containedTypeManagerContainerBuilder.build()
			);
		}
		catch (MappingAbortedException | RuntimeException e) {
			new SuppressingCloser( e )
					.push(
							PojoIndexedTypeManagerContainer.Builder::closeOnFailure,
							indexedTypeManagerContainerBuilder
					)
					.push(
							PojoContainedTypeManagerContainer.Builder::closeOnFailure,
							containedTypeManagerContainerBuilder
					);
			throw e;
		}
		closed = true;

		try {
			return wrapperFactory.apply( propertySource, mappingImplementor );
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e ).push( mappingImplementor );
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
			PojoContainedTypeManager<T> typeManager = new PojoContainedTypeManager<>(
					entityType.getJavaClass(), entityType.getCaster(), reindexingResolverOptional.get()
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

}
