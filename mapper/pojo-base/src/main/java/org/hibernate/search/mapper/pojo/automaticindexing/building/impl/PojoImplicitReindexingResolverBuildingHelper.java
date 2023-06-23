/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.building.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingAssociationInverseSideResolver;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingAssociationInverseSideResolverMarkingNode;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingAssociationInverseSideResolverNode;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolverImpl;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolverNode;
import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorBinder;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorHolder;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl.PojoTypeAdditionalMetadataProvider;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoEntityTypeAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoTypeAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.impl.PojoPathOrdinalReference;
import org.hibernate.search.mapper.pojo.model.path.impl.PojoPathOrdinals;
import org.hibernate.search.mapper.pojo.model.path.impl.PojoRuntimePathsBuildingHelper;
import org.hibernate.search.mapper.pojo.model.path.spi.BindablePojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoModelPathBinder;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathDefinition;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathEntityStateRepresentation;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.mapper.pojo.reporting.spi.PojoEventContexts;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class PojoImplicitReindexingResolverBuildingHelper {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ContainerExtractorBinder extractorBinder;
	private final PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider;
	private final PojoAssociationPathInverter pathInverter;
	private final Set<PojoRawTypeModel<?>> entityTypes;
	private final ReindexOnUpdate defaultReindexOnUpdate;

	private final Map<PojoRawTypeModel<?>, Set<PojoRawTypeModel<?>>> concreteEntitySubTypesByEntitySuperType =
			new HashMap<>();
	private final Map<PojoRawTypeModel<?>, PojoImplicitReindexingResolverBuilder<?>> builderByType =
			new HashMap<>();
	private final Map<PojoRawTypeModel<?>, PojoRuntimePathsBuildingHelper> runtimePathsBuildingHelperByType =
			new HashMap<>();

	public PojoImplicitReindexingResolverBuildingHelper(
			ContainerExtractorBinder extractorBinder,
			PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider,
			Set<PojoRawTypeModel<?>> entityTypes,
			ReindexOnUpdate defaultReindexOnUpdate) {
		this.extractorBinder = extractorBinder;
		this.typeAdditionalMetadataProvider = typeAdditionalMetadataProvider;
		this.pathInverter = new PojoAssociationPathInverter( typeAdditionalMetadataProvider, extractorBinder );
		this.entityTypes = entityTypes;
		this.defaultReindexOnUpdate = defaultReindexOnUpdate;

		for ( PojoRawTypeModel<?> entityType : entityTypes ) {
			if ( !entityType.isAbstract() ) {
				entityType.ascendingSuperTypes().forEach(
						superType -> concreteEntitySubTypesByEntitySuperType.computeIfAbsent(
								superType,
								// Use a LinkedHashSet for deterministic iteration
								ignored -> new LinkedHashSet<>()
						)
								.add( entityType )
				);
			}
		}
		// Make sure every Set is unmodifiable
		for ( Map.Entry<PojoRawTypeModel<?>, Set<PojoRawTypeModel<?>>> entry : concreteEntitySubTypesByEntitySuperType
				.entrySet() ) {
			entry.setValue( Collections.unmodifiableSet( entry.getValue() ) );
		}
	}

	public <T> PojoIndexingDependencyCollectorTypeNode<T> createDependencyCollector(PojoRawTypeModel<T> typeModel) {
		return new PojoIndexingDependencyCollectorTypeNode<>( typeModel, this );
	}

	public void closeOnFailure() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( PojoImplicitReindexingResolverBuilder::closeOnFailure, builderByType.values() );
		}
	}

	public <T> PojoImplicitReindexingResolver<T> build(PojoRawTypeModel<T> typeModel) {
		return buildOptional( typeModel )
				.orElseGet( () -> {
					PojoRuntimePathsBuildingHelper helper = runtimePathsBuildingHelper( typeModel );
					PojoPathFilter emptyFilter = helper.createFilter( Collections.emptySet() );
					return new PojoImplicitReindexingResolverImpl<>(
							emptyFilter, emptyFilter,
							PojoImplicitReindexingResolverNode.noOp(),
							createAssociationInverseSideResolver( typeModel, Collections.emptyMap() )
					);
				} );
	}

	public <T> Optional<PojoImplicitReindexingResolver<T>> buildOptional(PojoRawTypeModel<T> typeModel) {
		@SuppressWarnings("unchecked") // We know builders have this type, by construction
		PojoImplicitReindexingResolverBuilder<T> builder =
				(PojoImplicitReindexingResolverBuilder<T>) builderByType.get( typeModel );
		if ( builder == null ) {
			return Optional.empty();
		}
		else {
			return builder.build();
		}
	}

	public <T> PojoRuntimePathsBuildingHelper runtimePathsBuildingHelper(PojoRawTypeModel<T> typeModel) {
		return runtimePathsBuildingHelperByType.computeIfAbsent( typeModel, theTypeModel -> {
			PojoEntityTypeAdditionalMetadata entityTypeMetadata = typeAdditionalMetadataProvider.get( theTypeModel )
					.getEntityTypeMetadata()
					// This should not be possible since this method is only called for entity types (see callers)
					.orElseThrow( () -> new AssertionFailure( "Missing metadata for entity type '" + theTypeModel ) );
			return new PojoRuntimePathsBuildingHelper( entityTypeMetadata.pathDefinitionProvider() );
		} );
	}

	public PojoImplicitReindexingAssociationInverseSideResolver createAssociationInverseSideResolver(
			PojoRawTypeModel<?> typeModel,
			Map<PojoModelPathValueNode,
					Map<PojoRawTypeModel<?>, PojoModelPathValueNode>> inversePathByInverseTypeByDirectContainingPath) {
		PojoRuntimePathsBuildingHelper pathsBuildingHelper = runtimePathsBuildingHelper( typeModel );
		List<List<PojoImplicitReindexingAssociationInverseSideResolverNode<Object>>> resolversByOrdinal =
				createResolversByOrdinal( typeModel, pathsBuildingHelper, inversePathByInverseTypeByDirectContainingPath );
		PojoPathFilter filter = pathsBuildingHelper.createFilterForNonNullOrdinals( resolversByOrdinal );
		return new PojoImplicitReindexingAssociationInverseSideResolver(
				pathsBuildingHelper.pathOrdinals(), filter, resolversByOrdinal
		);
	}

	private List<List<PojoImplicitReindexingAssociationInverseSideResolverNode<Object>>> createResolversByOrdinal(
			PojoRawTypeModel<?> typeModel, PojoRuntimePathsBuildingHelper pathsBuildingHelper,
			Map<PojoModelPathValueNode,
					Map<PojoRawTypeModel<?>, PojoModelPathValueNode>> inversePathByInverseTypeByDirectContainingPath) {
		List<List<PojoImplicitReindexingAssociationInverseSideResolverNode<Object>>> result = new ArrayList<>();
		for ( Map.Entry<PojoModelPathValueNode,
				Map<PojoRawTypeModel<?>, PojoModelPathValueNode>> entry : inversePathByInverseTypeByDirectContainingPath
						.entrySet() ) {
			PojoModelPathValueNode path = entry.getKey();
			Map<PojoRawTypeModel<?>, PojoModelPathValueNode> inversePathByInverseType = entry.getValue();
			int ordinal;
			PojoImplicitReindexingAssociationInverseSideResolverNode<Object> nodeForOrdinal;
			try {
				// Bind the path to get a canonical representation (no "default extractors" in particular),
				// otherwise toPathDefinition() might lack some information and fail.
				BoundPojoModelPathValueNode<?, ?, ?> boundPath = bindPath( typeModel, path );
				PojoPathDefinition pathDefinition = pathsBuildingHelper.toPathDefinition( boundPath.toUnboundPath() );
				Optional<PojoPathEntityStateRepresentation> entityStateRepresentationOptional =
						pathDefinition.entityStateRepresentation();
				if ( !entityStateRepresentationOptional.isPresent() ) {
					// Ignore: we don't have metadata to resolve the inverse side of this association from entity state.
					// This may happen with the Standalone POJO Mapper,
					// but also for ToMany associations with the ORM Mapper (until we address HSEARCH-3567).
					continue;
				}
				PojoPathEntityStateRepresentation entityStateRepresentation = entityStateRepresentationOptional.get();
				ordinal = entityStateRepresentation.ordinalInStateArray();
				// Fill with nulls if necessary
				for ( int i = result.size(); i <= ordinal; i++ ) {
					result.add( null );
				}
				nodeForOrdinal = createAssociationInverseSideResolverNode(
						entityStateRepresentation.pathFromStateArrayElement(), inversePathByInverseType );
			}
			catch (RuntimeException e) {
				// We're logging instead of re-throwing for backwards compatibility,
				// as we don't want this feature to cause errors in existing applications.
				// TODO HSEARCH-4720 when we can afford breaking changes (in the next major), we should probably throw an exception
				//  instead of just logging a warning here?
				// Wrap the failure to append a message "please report this bug"
				AssertionFailure assertionFailure = e instanceof AssertionFailure
						? (AssertionFailure) e
						: new AssertionFailure( e.getMessage(), e );
				log.failedToCreateImplicitReindexingAssociationInverseSideResolverNode(
						inversePathByInverseType,
						EventContexts.fromType( typeModel ).append( PojoEventContexts.fromPath( path ) ),
						assertionFailure.getMessage(), assertionFailure );
				continue;
			}
			List<PojoImplicitReindexingAssociationInverseSideResolverNode<Object>> nodesForOrdinal = result.get( ordinal );
			if ( nodesForOrdinal == null ) {
				nodesForOrdinal = new ArrayList<>();
				result.set( ordinal, nodesForOrdinal );
			}
			nodesForOrdinal.add( nodeForOrdinal );
		}
		return result;
	}

	private PojoImplicitReindexingAssociationInverseSideResolverNode<Object> createAssociationInverseSideResolverNode(
			Optional<BindablePojoModelPath> pathFromStateArrayElementOptional,
			Map<PojoRawTypeModel<?>, PojoModelPathValueNode> inverseSide) {
		Map<PojoRawTypeIdentifier<?>, PojoPathOrdinalReference> ordinalByType = new HashMap<>();
		for ( Map.Entry<PojoRawTypeModel<?>, PojoModelPathValueNode> entry : inverseSide.entrySet() ) {
			PojoRawTypeModel<?> typeModel = entry.getKey();
			PojoModelPathValueNode path = entry.getValue();
			PojoRuntimePathsBuildingHelper inverseSideRuntimePathsHelper = runtimePathsBuildingHelper( typeModel );
			PojoPathOrdinals ordinals = inverseSideRuntimePathsHelper.pathOrdinals();
			int firstOrdinal = ordinals.toExistingOrNewOrdinal( inverseSideRuntimePathsHelper.toPathDefinition( path )
					.stringRepresentations().iterator().next() );
			ordinalByType.put( typeModel.typeIdentifier(), new PojoPathOrdinalReference( firstOrdinal, ordinals ) );
		}
		PojoImplicitReindexingAssociationInverseSideResolverMarkingNode markingNode =
				new PojoImplicitReindexingAssociationInverseSideResolverMarkingNode( ordinalByType );
		if ( !pathFromStateArrayElementOptional.isPresent() ) {
			return markingNode;
		}

		// Build a tree (well, a linked list, really) of nodes that will go through the given path
		BindablePojoModelPath pathFromStateArrayElement = pathFromStateArrayElementOptional.get();

		BoundPojoModelPathValueNode<?, ?, ?> boundPath = bindPath( pathFromStateArrayElement.rootType(),
				pathFromStateArrayElement.path() );

		return PojoImplicitReindexingAssociationInverseSideResolverNode.bind( extractorBinder, boundPath, markingNode );
	}

	public BoundPojoModelPathValueNode<?, ?, ?> bindPath(PojoTypeModel<?> rootType, PojoModelPathValueNode unboundPath) {
		return PojoModelPathBinder.bind( BoundPojoModelPath.root( rootType ),
				unboundPath, BoundPojoModelPath.walker( extractorBinder ) );
	}

	public boolean isSingleConcreteTypeInEntityHierarchy(PojoRawTypeModel<?> typeModel) {
		return typeModel.ascendingSuperTypes().filter( this::isEntity )
				.allMatch( t -> getConcreteEntitySubTypesForEntitySuperType( t ).size() <= 1 );
	}

	PojoAssociationPathInverter pathInverter() {
		return pathInverter;
	}

	boolean isEntity(PojoRawTypeModel<?> typeModel) {
		return entityTypes.contains( typeModel );
	}

	/**
	 * @return The set of concrete entity types that extend the given type.
	 * This is useful when building resolvers: when a type is the target of an indexed-embedded association,
	 * we generally want to take this information into account for every concrete subtype of that type,
	 * because the association could target any of them at runtime.
	 */
	Set<? extends PojoRawTypeModel<?>> getConcreteEntitySubTypesForEntitySuperType(PojoRawTypeModel<?> superTypeModel) {
		return concreteEntitySubTypesByEntitySuperType.computeIfAbsent( superTypeModel, ignored -> Collections.emptySet() );
	}

	<T> PojoImplicitReindexingResolverBuilder<T> getOrCreateResolverBuilder(
			PojoRawTypeModel<T> rawTypeModel) {
		@SuppressWarnings("unchecked") // We know builders have this type, by construction
		PojoImplicitReindexingResolverBuilder<T> builder =
				(PojoImplicitReindexingResolverBuilder<T>) builderByType.get( rawTypeModel );
		if ( builder == null ) {
			builder = new PojoImplicitReindexingResolverBuilder<>(
					rawTypeModel, this
			);
			builderByType.put( rawTypeModel, builder );
		}
		return builder;
	}

	ContainerExtractorBinder extractorBinder() {
		return extractorBinder;
	}

	<V, T> ContainerExtractorHolder<T, V> createExtractors(
			BoundContainerExtractorPath<T, V> boundExtractorPath) {
		return extractorBinder.create( boundExtractorPath );
	}

	ReindexOnUpdate getDefaultReindexOnUpdate() {
		return defaultReindexOnUpdate;
	}

	ReindexOnUpdate getMetadataReindexOnUpdateOrNull(PojoTypeModel<?> typeModel,
			String propertyName, ContainerExtractorPath extractorPath) {
		PojoTypeAdditionalMetadata typeAdditionalMetadata =
				typeAdditionalMetadataProvider.get( typeModel.rawType() );
		Optional<ReindexOnUpdate> reindexOnUpdateOptional =
				typeAdditionalMetadata.getPropertyAdditionalMetadata( propertyName )
						.getValueAdditionalMetadata( extractorPath )
						.getReindexOnUpdate();
		if ( reindexOnUpdateOptional.isPresent() ) {
			return reindexOnUpdateOptional.get();
		}

		if ( extractorBinder.isDefaultExtractorPath( typeModel.property( propertyName ).typeModel(), extractorPath ) ) {
			reindexOnUpdateOptional = typeAdditionalMetadata.getPropertyAdditionalMetadata( propertyName )
					.getValueAdditionalMetadata( ContainerExtractorPath.defaultExtractors() )
					.getReindexOnUpdate();
		}

		return reindexOnUpdateOptional.orElse( null );
	}

	Set<PojoModelPathValueNode> getMetadataDerivedFrom(PojoTypeModel<?> typeModel, String propertyName,
			ContainerExtractorPath extractorPath) {
		PojoTypeAdditionalMetadata typeAdditionalMetadata =
				typeAdditionalMetadataProvider.get( typeModel.rawType() );
		Set<PojoModelPathValueNode> derivedFrom =
				typeAdditionalMetadata.getPropertyAdditionalMetadata( propertyName )
						.getValueAdditionalMetadata( extractorPath )
						.getDerivedFrom();
		if ( derivedFrom.isEmpty() ) {
			if ( extractorBinder.isDefaultExtractorPath(
					typeModel.property( propertyName ).typeModel(),
					extractorPath
			) ) {
				derivedFrom = typeAdditionalMetadata.getPropertyAdditionalMetadata( propertyName )
						.getValueAdditionalMetadata( ContainerExtractorPath.defaultExtractors() )
						.getDerivedFrom();
			}
		}

		return derivedFrom;
	}
}
