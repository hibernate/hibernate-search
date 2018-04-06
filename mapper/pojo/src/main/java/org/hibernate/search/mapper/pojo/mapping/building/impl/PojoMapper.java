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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexManagerBuildingState;
import org.hibernate.search.engine.mapper.mapping.building.spi.Mapper;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.mapper.pojo.bridge.impl.BridgeResolver;
import org.hibernate.search.mapper.pojo.dirtiness.building.impl.PojoImplicitReindexingResolverBuildingHelper;
import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerValueExtractorBinder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoContainedTypeManager;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoContainedTypeManagerContainer;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoIndexedTypeManagerContainer;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoMappingDelegateImpl;
import org.hibernate.search.mapper.pojo.mapping.impl.ProvidedStringIdentifierMapping;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.model.augmented.building.impl.PojoAugmentedTypeModelProvider;
import org.hibernate.search.mapper.pojo.dirtiness.building.impl.PojoAssociationPathInverter;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.AssertionFailure;
import org.hibernate.search.util.impl.common.LoggerFactory;

class PojoMapper<M> implements Mapper<M> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ConfigurationPropertySource propertySource;
	private final TypeMetadataContributorProvider<PojoTypeMetadataContributor> contributorProvider;
	private final PojoBootstrapIntrospector introspector;
	private final boolean implicitProvidedId;
	private final BiFunction<ConfigurationPropertySource, PojoMappingDelegate, MappingImplementor<M>> wrapperFactory;
	private final PojoAugmentedTypeModelProvider augmentedTypeModelProvider;
	private final ContainerValueExtractorBinder extractorBinder;
	private final PojoMappingHelper mappingHelper;

	private final Map<PojoRawTypeModel<?>,PojoIndexedTypeManagerBuilder<?, ?>> indexedTypeManagerBuilders =
			new LinkedHashMap<>();

	PojoMapper(BuildContext buildContext, ConfigurationPropertySource propertySource,
			TypeMetadataContributorProvider<PojoTypeMetadataContributor> contributorProvider,
			PojoBootstrapIntrospector introspector,
			boolean implicitProvidedId,
			BiFunction<ConfigurationPropertySource, PojoMappingDelegate, MappingImplementor<M>> wrapperFactory) {
		this.propertySource = propertySource;
		this.contributorProvider = contributorProvider;
		this.introspector = introspector;
		this.implicitProvidedId = implicitProvidedId;
		this.wrapperFactory = wrapperFactory;

		augmentedTypeModelProvider = new PojoAugmentedTypeModelProvider( contributorProvider );
		extractorBinder = new ContainerValueExtractorBinder( buildContext );

		BridgeResolver bridgeResolver = new BridgeResolver();
		PojoIndexModelBinder indexModelBinder = new PojoIndexModelBinderImpl(
				buildContext, introspector, extractorBinder, bridgeResolver, augmentedTypeModelProvider
		);

		mappingHelper = new PojoMappingHelper(
				contributorProvider, indexModelBinder
		);
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
				entityTypeModel, mappingHelper, indexManagerBuildingState,
				implicitProvidedId ? ProvidedStringIdentifierMapping.get() : null );
		PojoMappingCollectorTypeNode collector = builder.asCollector();
		contributorProvider.forEach(
				entityTypeModel,
				c -> c.contributeMapping( collector )
		);
		indexedTypeManagerBuilders.put( entityTypeModel, builder );
	}

	@Override
	public MappingImplementor<M> build() {
		Set<PojoRawTypeModel<?>> entityTypes = computeEntityTypes();
		log.detectedEntityTypes( entityTypes );

		PojoIndexedTypeManagerContainer.Builder indexedTypeManagerContainerBuilder =
				PojoIndexedTypeManagerContainer.builder();
		PojoContainedTypeManagerContainer.Builder containedTypeManagerContainerBuilder =
				PojoContainedTypeManagerContainer.builder();
		PojoAssociationPathInverter pathInverter = new PojoAssociationPathInverter(
				augmentedTypeModelProvider, introspector, extractorBinder
		);
		PojoImplicitReindexingResolverBuildingHelper reindexingResolverBuildingHelper =
				new PojoImplicitReindexingResolverBuildingHelper(
						pathInverter, introspector, extractorBinder, entityTypes
				);

		// First phase: build the processors and contribute to the reindexing resolvers
		for ( PojoIndexedTypeManagerBuilder<?, ?> pojoIndexedTypeManagerBuilder : indexedTypeManagerBuilders.values() ) {
			pojoIndexedTypeManagerBuilder.preBuild( reindexingResolverBuildingHelper );
		}
		// Second phase: build the indexed type managers and their reindexing resolvers
		for ( PojoIndexedTypeManagerBuilder<?, ?> b : indexedTypeManagerBuilders.values() ) {
			b.buildAndAddTo( indexedTypeManagerContainerBuilder, reindexingResolverBuildingHelper );
		}
		// Third phase: build the non-indexed, contained type managers and their reindexing resolvers
		for ( PojoRawTypeModel<?> entityType : entityTypes ) {
			// Ignore abstract classes: we create one manager per concrete subclass, which is enough
			if ( !entityType.isAbstract() && !indexedTypeManagerBuilders.containsKey( entityType ) ) {
				buildAndAddContainedTypeManagerTo(
						containedTypeManagerContainerBuilder, reindexingResolverBuildingHelper, entityType
				);
			}
		}

		PojoMappingDelegate mappingImplementor = new PojoMappingDelegateImpl(
				indexedTypeManagerContainerBuilder.build(),
				containedTypeManagerContainerBuilder.build()
		);
		return wrapperFactory.apply( propertySource, mappingImplementor );
	}

	private <T> void buildAndAddContainedTypeManagerTo(
			PojoContainedTypeManagerContainer.Builder containedTypeManagerContainerBuilder,
			PojoImplicitReindexingResolverBuildingHelper reindexingResolverBuildingHelper,
			PojoRawTypeModel<T> entityType) {
		Optional<? extends PojoImplicitReindexingResolver<T>> reindexingResolverOptional =
				reindexingResolverBuildingHelper.build( entityType );
		if ( reindexingResolverOptional.isPresent() ) {
			PojoContainedTypeManager<T> typeManager = new PojoContainedTypeManager<>(
					entityType.getJavaClass(), entityType.getCaster(), reindexingResolverOptional.get()
			);
			log.createdPojoContainedTypeManager( typeManager );
			containedTypeManagerContainerBuilder.add( entityType, typeManager );
		}
	}

	private Set<PojoRawTypeModel<?>> computeEntityTypes() {
		Set<PojoRawTypeModel<?>> entityTypes = new HashSet<>();
		Collection<? extends MappableTypeModel> encounteredTypes = contributorProvider.getTypesContributedTo();
		for ( MappableTypeModel mappableTypeModel : encounteredTypes ) {
			PojoRawTypeModel<?> pojoRawTypeModel = (PojoRawTypeModel<?>) mappableTypeModel;
			if ( augmentedTypeModelProvider.get( pojoRawTypeModel ).isEntity() ) {
				entityTypes.add( pojoRawTypeModel );
			}
		}
		return Collections.unmodifiableSet( entityTypes );
	}

}
