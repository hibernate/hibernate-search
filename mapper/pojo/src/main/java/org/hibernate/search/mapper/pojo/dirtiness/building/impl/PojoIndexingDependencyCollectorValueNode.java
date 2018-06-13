/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.dirtiness.building.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoImplicitReindexingResolverNode;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoTypeAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.AssertionFailure;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * A node representing a value in a dependency collector.
 * <p>
 * The role of dependency collectors is to receive the dependencies (paths to values used during indexing)
 * as an input, and use this information to contribute to
 * {@link PojoImplicitReindexingResolverNode}s.
 * This involves in particular:
 * <ul>
 *     <li>Resolving the path from one entity to another, "contained" entity</li>
 *     <li>Resolving the potential concrete type of "containing" entities</li>
 *     <li>Resolving the potential concrete type of "contained" entities</li>
 *     <li>Inverting the path from one entity to another using a {@link PojoAssociationPathInverter}
 *     <li>Applying the inverted path to the reindexing resolver builder of the "contained" entity,
 *     so that whenever the "contained" entity is modified, the "containing" entity is reindexed.
 *     </li>
 * </ul>
 *
 * @see PojoIndexingDependencyCollectorTypeNode
 *
 * @param <P> The property type
 * @param <V> The extracted value type
 */
public class PojoIndexingDependencyCollectorValueNode<P, V> extends AbstractPojoIndexingDependencyCollectorNode {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final BoundPojoModelPathValueNode<?, P, V> modelPathFromRootEntityNode;
	private final PojoIndexingDependencyCollectorTypeNode<?> lastEntityNode;
	private final BoundPojoModelPathValueNode<?, P, V> modelPathFromLastEntityNode;

	// First key: inverse side entity type, second key: original side concrete entity type
	private Map<PojoRawTypeModel<?>, Map<PojoRawTypeModel<?>, PojoModelPathValueNode>> inverseAssociationPathCache =
			new HashMap<>();

	PojoIndexingDependencyCollectorValueNode(BoundPojoModelPathValueNode<?, P, V> modelPathFromRootEntityNode,
			PojoIndexingDependencyCollectorTypeNode<?> lastEntityNode,
			BoundPojoModelPathValueNode<?, P, V> modelPathFromLastEntityNode,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		super( buildingHelper );
		this.modelPathFromRootEntityNode = modelPathFromRootEntityNode;
		this.lastEntityNode = lastEntityNode;
		this.modelPathFromLastEntityNode = modelPathFromLastEntityNode;
	}

	public PojoIndexingDependencyCollectorTypeNode<V> type() {
		return new PojoIndexingDependencyCollectorTypeNode<>(
				this, modelPathFromRootEntityNode.type(), lastEntityNode, modelPathFromLastEntityNode.type(), buildingHelper
		);
	}

	public void collectDependency() {
		lastEntityNode.collectDependency( modelPathFromLastEntityNode );
	}

	/**
	 * @param inverseSideEntityTypeNodeBuilder A type node builder representing the type of this value as viewed from the contained side.
	 * Its type must be a subtype of the raw type of this value.
	 * Its type must be an {@link PojoTypeAdditionalMetadata#isEntity() entity type}.
	 * @param dependencyPathFromInverseSideEntityTypeNode The path from the given entity type node
	 * to the property being used when reindexing.
	 */
	void markForReindexing(AbstractPojoImplicitReindexingResolverTypeNodeBuilder<?, ?> inverseSideEntityTypeNodeBuilder,
			BoundPojoModelPathValueNode<?, ?, ?> dependencyPathFromInverseSideEntityTypeNode) {
		PojoTypeModel<?> inverseSideEntityType = inverseSideEntityTypeNodeBuilder.getTypeModel();
		PojoRawTypeModel<?> inverseSideRawEntityType = inverseSideEntityType.getRawType();
		PojoTypeModel<V> expectedInverseSideEntityType = modelPathFromRootEntityNode.type().getTypeModel();
		PojoRawTypeModel<?> expectedInverseSideEntityRawType = expectedInverseSideEntityType.getRawType();
		if ( !inverseSideRawEntityType.isSubTypeOf( expectedInverseSideEntityRawType ) ) {
			throw new AssertionFailure(
					"Error while building the automatic reindexing resolver at path " + modelPathFromRootEntityNode
							+ ": the dependency collector was passed a resolver builder with incorrect type; "
							+ " got " + inverseSideRawEntityType + ", but a subtype of " + expectedInverseSideEntityRawType
							+ " was expected."
							+ " This is very probably a bug in Hibernate Search, please report it."
			);
		}

		Map<PojoRawTypeModel<?>, PojoModelPathValueNode> inverseAssociationsPaths =
				getInverseAssociationPathByConcreteEntityType( inverseSideEntityTypeNodeBuilder.getTypeModel() );
		for ( Map.Entry<PojoRawTypeModel<?>, PojoModelPathValueNode> entry : inverseAssociationsPaths.entrySet() ) {
			markForReindexingWithOriginalSideConcreteType(
					entry.getKey(), inverseSideEntityTypeNodeBuilder, entry.getValue(),
					dependencyPathFromInverseSideEntityTypeNode
			);
		}
	}

	private Map<PojoRawTypeModel<?>, PojoModelPathValueNode> getInverseAssociationPathByConcreteEntityType(
			PojoTypeModel<?> inverseSideEntityType) {
		PojoRawTypeModel<?> inverseSideRawEntityType = inverseSideEntityType.getRawType();

		// Cache the inverse association path, as we may compute it many times, which may be costly
		Map<PojoRawTypeModel<?>, PojoModelPathValueNode> result = inverseAssociationPathCache.get( inverseSideRawEntityType );
		if ( result == null ) {
			if ( !inverseAssociationPathCache.containsKey( inverseSideRawEntityType ) ) {
				PojoTypeModel<?> originalSideEntityType = lastEntityNode.getTypeModel();
				PojoRawTypeModel<?> originalSideRawEntityType = originalSideEntityType.getRawType();

				// Use a LinkedHashMap for deterministic iteration
				result = new LinkedHashMap<>();

				for ( PojoRawTypeModel<?> concreteEntityType :
						buildingHelper.getConcreteEntitySubTypesForEntitySuperType( originalSideRawEntityType ) ) {
					BoundPojoModelPathValueNode<?, ?, ?> modelPathFromConcreteEntitySubType =
							applyProcessingPathToSubType( concreteEntityType, modelPathFromLastEntityNode );
					PojoModelPathValueNode inverseAssociationPath = buildingHelper.getPathInverter()
							.invertPath( inverseSideEntityType, modelPathFromConcreteEntitySubType )
							.orElse( null );
					if ( inverseAssociationPath == null ) {
						throw log.cannotInvertAssociation(
								inverseSideRawEntityType, concreteEntityType,
								modelPathFromLastEntityNode.toUnboundPath()
						);
					}

					result.put( concreteEntityType, inverseAssociationPath );
				}

				inverseAssociationPathCache.put( inverseSideRawEntityType, result );
			}
			else {
				// Only report the first error, then ignore silently
				result = Collections.emptyMap();
			}
		}

		return result;
	}

	private void markForReindexingWithOriginalSideConcreteType(PojoTypeModel<?> originalSideConcreteEntityType,
			AbstractPojoImplicitReindexingResolverTypeNodeBuilder<?, ?> typeNodeBuilder,
			PojoModelPathValueNode inverseAssociationPath,
			BoundPojoModelPathValueNode<?, ?, ?> dependencyPathFromInverseSideEntityTypeNode) {
		PojoTypeModel<?> inverseSideEntityType = typeNodeBuilder.getTypeModel();
		PojoRawTypeModel<?> inverseSideRawEntityType = inverseSideEntityType.getRawType();

		PojoRawTypeModel<?> originalSideRawConcreteEntityType = originalSideConcreteEntityType.getRawType();

		// Attempt to apply the inverse association path to the given builder
		PojoImplicitReindexingResolverValueNodeBuilderDelegate<?> valueNodeBuilderDelegate;
		Set<? extends PojoRawTypeModel<?>> valueNodeTypeConcreteEntitySubTypes;
		try {
			valueNodeBuilderDelegate = applyPath( typeNodeBuilder, inverseAssociationPath );

			/*
			 * The entities to reindex will always be instances of both the entity type on the original side
			 * (because that's the one we want to reindex)
			 * and the type targeted by the inverse side of the association
			 * (because that's all we will ever retrieve at runtime).
			 * Thus we will only consider the most specific type of the two when resolving entities to reindex.
			 */
			PojoRawTypeModel<?> valueNodeRawType = valueNodeBuilderDelegate.getTypeModel().getRawType();
			if ( valueNodeRawType.isSubTypeOf( originalSideRawConcreteEntityType ) ) {
				valueNodeTypeConcreteEntitySubTypes =
						buildingHelper.getConcreteEntitySubTypesForEntitySuperType( valueNodeRawType );
			}
			else if ( originalSideRawConcreteEntityType.isSubTypeOf( valueNodeRawType ) ) {
				valueNodeTypeConcreteEntitySubTypes =
						buildingHelper.getConcreteEntitySubTypesForEntitySuperType( originalSideRawConcreteEntityType );
			}
			else {
				throw log.incorrectTargetTypeForInverseAssociation( valueNodeRawType, originalSideRawConcreteEntityType );
			}
		}
		// Note: this should catch errors related to properties not found, among others.
		catch (RuntimeException e) {
			throw log.cannotApplyInvertAssociationPath(
					inverseSideRawEntityType, inverseAssociationPath,
					originalSideRawConcreteEntityType, modelPathFromLastEntityNode.toUnboundPath(),
					e.getMessage(), e
			);
		}

		// Recurse if necessary
		PojoIndexingDependencyCollectorValueNode<?, ?> entityNodeParentValueNode = lastEntityNode.getParentNode();
		if ( entityNodeParentValueNode != null ) {
			/*
			 * We did not reach the indexed type yet.
			 * Continue to build the inverse path from the "potentially dirty" value to the indexed type.
			 */
			for ( PojoRawTypeModel<?> concreteEntityType : valueNodeTypeConcreteEntitySubTypes ) {
				AbstractPojoImplicitReindexingResolverTypeNodeBuilder<?, ?> inverseValueTypeBuilder =
						valueNodeBuilderDelegate.type( concreteEntityType );
				entityNodeParentValueNode.markForReindexing(
						inverseValueTypeBuilder, dependencyPathFromInverseSideEntityTypeNode
				);
			}
		}
		else {
			/*
			 * We fully built the inverse path from the "potentially dirty" entity to the indexed type.
			 * Mark the values at the end of that inverse path as requiring reindexing
			 * when the entity holding the inverse path is dirty on the given dependency path.
			 */
			for ( PojoRawTypeModel<?> concreteEntityType : valueNodeTypeConcreteEntitySubTypes ) {
				AbstractPojoImplicitReindexingResolverTypeNodeBuilder<?, ?> inverseValueTypeBuilder =
						valueNodeBuilderDelegate.type( concreteEntityType );
				inverseValueTypeBuilder.addDirtyPathTriggeringReindexing(
						dependencyPathFromInverseSideEntityTypeNode
				);
			}
		}
	}

	private PojoImplicitReindexingResolverValueNodeBuilderDelegate<?> applyPath(
			AbstractPojoImplicitReindexingResolverTypeNodeBuilder<?, ?> builder,
			PojoModelPathValueNode unboundPath) {
		PojoImplicitReindexingResolverPropertyNodeBuilder<?, ?> propertyNodeBuilder =
				applyPath( builder, unboundPath.getParent() );
		ContainerValueExtractorPath inverseExtractorPath = unboundPath.getExtractorPath();
		return propertyNodeBuilder.value( inverseExtractorPath );
	}

	private PojoImplicitReindexingResolverPropertyNodeBuilder<?, ?> applyPath(
			AbstractPojoImplicitReindexingResolverTypeNodeBuilder<?, ?> builder,
			PojoModelPathPropertyNode unboundPath) {
		PojoModelPathValueNode parent = unboundPath.getParent();
		AbstractPojoImplicitReindexingResolverTypeNodeBuilder<?, ?> parentBuilder;
		if ( parent != null ) {
			parentBuilder = applyPath( builder, parent ).type();
		}
		else {
			parentBuilder = builder;
		}
		String inversePropertyName = unboundPath.getPropertyName();
		return parentBuilder.property( inversePropertyName );
	}

	private BoundPojoModelPathValueNode<?, ?, ?> applyProcessingPathToSubType(PojoRawTypeModel<?> rootSubType,
			BoundPojoModelPathValueNode<?, ?, ?> source) {
		BoundPojoModelPathPropertyNode<?, ?> targetParent = applyProcessingPathToSubType( rootSubType, source.getParent() );
		return bindAndApplyExtractorPath( targetParent, source.getExtractorPath() );
	}

	private BoundPojoModelPathPropertyNode<?, ?> applyProcessingPathToSubType(PojoRawTypeModel<?> rootSubType,
			BoundPojoModelPathPropertyNode<?, ?> source) {
		BoundPojoModelPathTypeNode<?> targetParent = applyProcessingPathToSubType( rootSubType, source.getParent() );
		return targetParent.property( source.getPropertyHandle() );
	}

	private BoundPojoModelPathTypeNode<?> applyProcessingPathToSubType(PojoRawTypeModel<?> rootSubType,
			BoundPojoModelPathTypeNode<?> source) {
		BoundPojoModelPathValueNode<?, ?, ?> sourceParent = source.getParent();
		if ( sourceParent != null ) {
			return applyProcessingPathToSubType( rootSubType, sourceParent ).type();
		}
		else {
			return BoundPojoModelPath.root( rootSubType );
		}
	}

	private <T, P> BoundPojoModelPathValueNode<T, P, ?> bindAndApplyExtractorPath(
			BoundPojoModelPathPropertyNode<T, P> propertyNode, ContainerValueExtractorPath extractorPath) {
		BoundContainerValueExtractorPath<P, ?> boundExtractorPath =
				buildingHelper.bindExtractorPath( propertyNode.getPropertyModel().getTypeModel(), extractorPath );
		return propertyNode.value( boundExtractorPath );
	}

}
