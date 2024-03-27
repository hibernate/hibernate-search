/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.automaticindexing.building.impl;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorBinder;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl.PojoTypeAdditionalMetadataProvider;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoPropertyAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoTypeAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoValueAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * An object responsible for inverting an association path,
 * i.e. a chain of properties and container value extractors going from one entity to another.
 */
final class PojoAssociationPathInverter {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider;
	private final ContainerExtractorBinder extractorBinder;

	PojoAssociationPathInverter(PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider,
			ContainerExtractorBinder extractorBinder) {
		this.typeAdditionalMetadataProvider = typeAdditionalMetadataProvider;
		this.extractorBinder = extractorBinder;
	}

	public Optional<PojoModelPathValueNode> invertPath(PojoTypeModel<?> inverseSideEntityType,
			BoundPojoModelPathValueNode<?, ?, ?> pathToInvert) {
		PojoRawTypeModel<?> originalSideEntityType = pathToInvert.getRootType().rawType();

		// Try to find inverse side information hosted on the side to inverse
		Optional<PojoModelPathValueNode> inverseSidePathOptional =
				findInverseSidePathFromOriginalSide( pathToInvert );

		if ( !inverseSidePathOptional.isPresent() ) {
			List<PojoModelPathValueNode> associationPathsToMatch = computeAssociationPathsToMatch( pathToInvert );
			// Try to find inverse side information hosted on the other side
			inverseSidePathOptional = findInverseSidePathFromInverseSide(
					inverseSideEntityType, originalSideEntityType, associationPathsToMatch
			);
		}

		return inverseSidePathOptional;
	}

	/*
	 * One might refer to the extractor path of an association in multiple ways:
	 * - By intension, e.g. ContainerExtractorPath.default()
	 * - By extension, e.g. ContainerExtractorPath.noExtractors()
	 *   or ContainerExtractorPath.explicitExtractors( ... )
	 * We want to match any type of reference, so we have to determine whether this association
	 * uses the default extractor path, and if so, add it to the list of paths to match.
	 */
	private List<PojoModelPathValueNode> computeAssociationPathsToMatch(
			BoundPojoModelPathValueNode<?, ?, ?> boundPathToInvert) {
		// We're potentially performing lots of insertions, so let's use a LinkedList
		List<PojoModelPathValueNode> associationPathsToMatch = new LinkedList<>();
		collectAssociationPathsToMatch( associationPathsToMatch, boundPathToInvert );
		return associationPathsToMatch;
	}

	private void collectAssociationPathsToMatch(
			List<PojoModelPathValueNode> associationPathsToMatch,
			BoundPojoModelPathValueNode<?, ?, ?> boundPathToInvert) {
		BoundPojoModelPathPropertyNode<?, ?> parentPath = boundPathToInvert.getParent();
		BoundPojoModelPathValueNode<?, ?, ?> parentValuePath = parentPath.getParent().getParent();
		String propertyName = parentPath.getPropertyModel().name();
		ContainerExtractorPath extractorPath = boundPathToInvert.getExtractorPath();
		boolean isDefaultExtractorPath = isDefaultExtractorPath(
				parentPath.getPropertyModel(), boundPathToInvert.getBoundExtractorPath()
		);
		if ( parentValuePath != null ) {
			collectAssociationPathsToMatch( associationPathsToMatch, parentValuePath );
			ListIterator<PojoModelPathValueNode> iterator = associationPathsToMatch.listIterator();
			while ( iterator.hasNext() ) {
				PojoModelPathValueNode baseValuePath = iterator.next();
				PojoModelPathPropertyNode basePropertyPath = baseValuePath.property( propertyName );
				// Append the property and extractor path to the already-collected paths
				iterator.set( basePropertyPath.value( extractorPath ) );
				if ( isDefaultExtractorPath ) {
					/*
					 * If the current extractor path (which is explicit) represents the default path,
					 * then for each already collected path, add one version using the explicit representation,
					 * and one version using the implicit representation.
					 */
					iterator.add( basePropertyPath.value( ContainerExtractorPath.defaultExtractors() ) );
				}
			}
		}
		else {
			// We reached the root: collect the first paths
			associationPathsToMatch.add( PojoModelPath.ofValue( propertyName, extractorPath ) );
			if ( isDefaultExtractorPath ) {
				// The may be two versions of this path, similarly to what we do above
				associationPathsToMatch
						.add( PojoModelPath.ofValue( propertyName, ContainerExtractorPath.defaultExtractors() ) );
			}
		}
	}

	private boolean isDefaultExtractorPath(PojoPropertyModel<?> propertyModel,
			BoundContainerExtractorPath<?, ?> originalSideBoundExtractorPath) {
		return extractorBinder.isDefaultExtractorPath(
				propertyModel.typeModel(),
				originalSideBoundExtractorPath.getExtractorPath()
		);
	}

	private Optional<PojoModelPathValueNode> findInverseSidePathFromOriginalSide(
			BoundPojoModelPathValueNode<?, ?, ?> pathToInvert) {
		BoundPojoModelPathPropertyNode<?, ?> lastPropertyNode = pathToInvert.getParent();
		BoundPojoModelPathTypeNode<?> lastTypeNode = lastPropertyNode.getParent();
		PojoPropertyModel<?> lastPropertyModel = lastPropertyNode.getPropertyModel();
		PojoTypeModel<?> lastTypeModel = lastTypeNode.getTypeModel();

		PojoTypeAdditionalMetadata typeAdditionalMetadata =
				typeAdditionalMetadataProvider.get( lastTypeModel.rawType() );
		PojoPropertyAdditionalMetadata propertyAdditionalMetadata =
				typeAdditionalMetadata.getPropertyAdditionalMetadata( lastPropertyNode.getPropertyModel().name() );

		// First try to query the additional metadata with the explicit extractor path
		Optional<PojoModelPathValueNode> result =
				propertyAdditionalMetadata.getValueAdditionalMetadata( pathToInvert.getExtractorPath() )
						.getInverseSidePath();
		if ( result.isPresent() ) {
			return result;
		}

		if ( isDefaultExtractorPath( lastPropertyModel, pathToInvert.getBoundExtractorPath() ) ) {
			/*
			 * Since the extractor path was the default one, try to query the additional metadata
			 * with the implicit default extractor path.
			 */
			result = propertyAdditionalMetadata.getValueAdditionalMetadata( ContainerExtractorPath.defaultExtractors() )
					.getInverseSidePath();
		}

		return result;
	}

	private Optional<PojoModelPathValueNode> findInverseSidePathFromInverseSide(
			PojoTypeModel<?> inverseSideTypeModel,
			PojoRawTypeModel<?> originalSideEntityType,
			List<PojoModelPathValueNode> associationPathsToMatch) {
		BoundPojoModelPathTypeNode<?> inverseSidePathTypeNode = BoundPojoModelPath.root( inverseSideTypeModel );
		Set<PojoRawTypeModel<?>> encounteredAssociationHoldingTypes = new HashSet<>();
		return findInverseSidePathFromInverseSideRecursive(
				inverseSidePathTypeNode, originalSideEntityType, associationPathsToMatch,
				encounteredAssociationHoldingTypes
		);
	}

	private Optional<PojoModelPathValueNode> findInverseSidePathFromInverseSideRecursive(
			BoundPojoModelPathTypeNode<?> inverseSidePathTypeNode,
			PojoRawTypeModel<?> originalSideEntityType,
			List<PojoModelPathValueNode> associationPathsToMatch,
			Set<PojoRawTypeModel<?>> encounteredAssociationHoldingTypes) {
		PojoTypeModel<?> inverseSideTypeModel = inverseSidePathTypeNode.getTypeModel();
		PojoTypeAdditionalMetadata inverseSideTypeAdditionalMetadata =
				typeAdditionalMetadataProvider.get( inverseSideTypeModel.rawType() );

		for ( String inverseSidePropertyName : inverseSideTypeAdditionalMetadata
				.getNamesOfPropertiesWithAdditionalMetadata() ) {
			BoundPojoModelPathPropertyNode<?, ?> inverseSidePathPropertyNode =
					inverseSidePathTypeNode.property( inverseSidePropertyName );
			PojoPropertyAdditionalMetadata inverseSidePropertyAdditionalMetadata =
					inverseSideTypeAdditionalMetadata.getPropertyAdditionalMetadata( inverseSidePropertyName );

			for ( Map.Entry<ContainerExtractorPath,
					PojoValueAdditionalMetadata> valueEntry : inverseSidePropertyAdditionalMetadata
							.getValuesAdditionalMetadata().entrySet() ) {
				ContainerExtractorPath inverseSideExtractorPath = valueEntry.getKey();
				BoundPojoModelPathValueNode<?, ?, ?> inverseSidePathValueNode =
						bindExtractors( inverseSidePathPropertyNode, inverseSideExtractorPath );
				PojoValueAdditionalMetadata inverseSideValueAdditionalMetadata = valueEntry.getValue();

				Optional<PojoModelPathValueNode> inverseSidePathOptional =
						findInverseSidePathFromInverseSideValueRecursive(
								originalSideEntityType, associationPathsToMatch,
								inverseSidePathValueNode, inverseSideValueAdditionalMetadata,
								encounteredAssociationHoldingTypes
						);

				if ( inverseSidePathOptional.isPresent() ) {
					return inverseSidePathOptional;
				}
				// else: continue the loop, maybe we'll find the inverse path elsewhere.
			}
		}

		return Optional.empty();
	}

	private Optional<PojoModelPathValueNode> findInverseSidePathFromInverseSideValueRecursive(
			PojoRawTypeModel<?> originalSideEntityType,
			List<PojoModelPathValueNode> associationPathsToMatch,
			BoundPojoModelPathValueNode<?, ?, ?> inverseSidePathValueNode,
			PojoValueAdditionalMetadata inverseSideValueAdditionalMetadata,
			Set<PojoRawTypeModel<?>> encounteredAssociationHoldingTypes) {
		Optional<PojoModelPathValueNode> candidatePathOptional =
				inverseSideValueAdditionalMetadata.getInverseSidePath();

		PojoRawTypeModel<?> rawExtractedTypeModel =
				inverseSidePathValueNode.type().getTypeModel().rawType();

		if ( candidatePathOptional.isPresent()
				&& associationPathsToMatch.contains( candidatePathOptional.get() ) ) {
			PojoModelPathValueNode inverseAssociationPath = inverseSidePathValueNode.toUnboundPath();
			/*
			 * In order to match, the inverse path, when applied to the inverse entity type,
			 * must also result in a supertype of the entity type holding the association to invert.
			 * This is to handle cases where an entity holds inverse associations of multiple associations
			 * from multiple different entities: in that case, the "original" associations may have
			 * the same name and extractors.
			 */
			if ( originalSideEntityType.isSubTypeOf( rawExtractedTypeModel ) ) {
				return Optional.of( inverseAssociationPath );
			}
		}

		if ( inverseSideValueAdditionalMetadata.isAssociationEmbedded() ) {
			if ( encounteredAssociationHoldingTypes.contains( rawExtractedTypeModel ) ) {
				throw log.infiniteRecursionForAssociationEmbeddeds(
						inverseSidePathValueNode.getRootType().rawType(),
						inverseSidePathValueNode.toUnboundPath()
				);
			}

			encounteredAssociationHoldingTypes.add( rawExtractedTypeModel );
			candidatePathOptional = findInverseSidePathFromInverseSideRecursive(
					inverseSidePathValueNode.type(), originalSideEntityType, associationPathsToMatch,
					encounteredAssociationHoldingTypes
			);
			encounteredAssociationHoldingTypes.remove( rawExtractedTypeModel );
			if ( candidatePathOptional.isPresent() ) {
				return candidatePathOptional;
			}
		}

		return Optional.empty();
	}

	private <P> BoundPojoModelPathValueNode<?, P, ?> bindExtractors(
			BoundPojoModelPathPropertyNode<?, P> inverseSidePathPropertyNode,
			ContainerExtractorPath extractorPath) {
		BoundContainerExtractorPath<P, ?> resolvedExtractorPath =
				extractorBinder.bindPath(
						inverseSidePathPropertyNode.getPropertyModel().typeModel(),
						extractorPath
				);
		return inverseSidePathPropertyNode.value( resolvedExtractorPath );
	}
}
