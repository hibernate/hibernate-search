/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.building.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolverNode;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.binding.impl.PojoModelPathBinder;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

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
public class PojoIndexingDependencyCollectorValueNode<P, V>
		extends AbstractPojoIndexingDependencyCollectorValueNode {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoIndexingDependencyCollectorPropertyNode<?, P> parentNode;
	/**
	 * The path to this node from the last type node, i.e. from the node
	 * representing the type holding the property from which this value is extracted.
	 */
	private final BoundPojoModelPathValueNode<?, P, V> modelPathFromLastTypeNode;
	private final PojoModelPathValueNode unboundModelPathFromLastTypeNode;
	private final BoundPojoModelPathValueNode<?, P, V> modelPathFromLastEntityNode;

	private final ReindexOnUpdate reindexOnUpdate;
	private final Set<PojoModelPathValueNode> derivedFrom;

	// First key: inverse side entity type, second key: original side concrete entity type
	private final Map<PojoRawTypeModel<?>, Map<PojoRawTypeModel<?>, PojoModelPathValueNode>> inverseAssociationPathCache =
			new HashMap<>();

	PojoIndexingDependencyCollectorValueNode(PojoIndexingDependencyCollectorPropertyNode<?, P> parentNode,
			BoundPojoModelPathValueNode<?, P, V> modelPathFromLastTypeNode,
			BoundPojoModelPathValueNode<?, P, V> modelPathFromLastEntityNode,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		super( buildingHelper );
		this.parentNode = parentNode;
		this.modelPathFromLastTypeNode = modelPathFromLastTypeNode;
		// The path is used for comparisons (equals), so we need it unbound
		this.unboundModelPathFromLastTypeNode = modelPathFromLastTypeNode.toUnboundPath();
		this.modelPathFromLastEntityNode = modelPathFromLastEntityNode;

		BoundPojoModelPathValueNode<?, P, V> modelPathValueNode = modelPathFromLastTypeNode;
		BoundPojoModelPathPropertyNode<?, P> modelPathPropertyNode = modelPathFromLastTypeNode.getParent();
		BoundPojoModelPathTypeNode<?> modelPathTypeNode = modelPathPropertyNode.getParent();
		ReindexOnUpdate metadataReindexOnUpdateOrNull = buildingHelper.getMetadataReindexOnUpdateOrNull(
				modelPathTypeNode.getTypeModel(), modelPathPropertyNode.getPropertyModel().name(),
				modelPathValueNode.getExtractorPath() );
		this.reindexOnUpdate = parentNode.composeReindexOnUpdate( lastEntityNode(), metadataReindexOnUpdateOrNull );
		this.derivedFrom = buildingHelper.getMetadataDerivedFrom(
				modelPathTypeNode.getTypeModel(),
				modelPathPropertyNode.getPropertyModel().name(),
				modelPathValueNode.getExtractorPath()
		);
	}

	public PojoIndexingDependencyCollectorTypeNode<V> type() {
		return new PojoIndexingDependencyCollectorTypeNode<>(
				this,
				modelPathFromLastEntityNode.type(),
				buildingHelper
		);
	}

	public <U> PojoIndexingDependencyCollectorTypeNode<U> castedType(PojoRawTypeModel<U> typeModel) {
		return new PojoIndexingDependencyCollectorTypeNode<>(
				this,
				modelPathFromLastEntityNode.castedType( typeModel ),
				buildingHelper
		);
	}

	public void collectDependency() {
		doCollectDependency( null );
	}

	@Override
	void collectDependency(BoundPojoModelPathValueNode<?, ?, ?> dirtyPathFromEntityType) {
		if ( derivedFrom.isEmpty() ) {
			parentNode.parentNode().collectDependency( dirtyPathFromEntityType );
		}
		else {
			// This value is derived from other properties.
			// Any part of this value is assumed to be derived from the same properties:
			// we don't care about which part in particular.
			collectDependency();
		}
	}

	void doCollectDependency(PojoIndexingDependencyCollectorValueNode<?, ?> initialNodeCollectingDependency) {
		// See the handling of derived properties below to get an idea of
		// what "reindexOnUpdateFromDerivedProperty" is for:
		// essentially it allows marking a derived property with ReindexOnUpdate.SHALLOW
		// so that, in the context of that derived property,
		// the properties it's derived from are handled as SHALLOW,
		// i.e. we only collect dependency up to entity boundaries.
		ReindexOnUpdate composedReindexOnUpdate = initialNodeCollectingDependency == null ? reindexOnUpdate
				: initialNodeCollectingDependency.composeReindexOnUpdate( lastEntityNode(), reindexOnUpdate );
		if ( ReindexOnUpdate.NO.equals( composedReindexOnUpdate ) ) {
			// Updates are ignored
			return;
		}

		if ( initialNodeCollectingDependency != null ) {
			PojoRawTypeModel<?> initialType = initialNodeCollectingDependency.modelPathFromLastTypeNode
					.getRootType().rawType();
			PojoModelPathValueNode initialValuePath = initialNodeCollectingDependency.unboundModelPathFromLastTypeNode;
			PojoRawTypeModel<?> latestType = modelPathFromLastTypeNode.getRootType().rawType();
			PojoModelPathValueNode latestValuePath = unboundModelPathFromLastTypeNode;
			if ( initialType.equals( latestType ) && initialValuePath.equals( latestValuePath ) ) {
				/*
				 * We found a cycle in the derived from dependencies.
				 * This can happen for example if:
				 * - property "foo" on type A is marked as derived from itself
				 * - property "foo" on type A is marked as derived from property "bar" on type B,
				 *   which is marked as derived from property "foo" on type "A".
				 * Even if such a dependency might work in practice at runtime,
				 * for example because the link A => B never leads to a B that refers to the same A,
				 * even indirectly,
				 * we cannot support it here because we need to model dependencies as a static tree,
				 * which in such case would have an infinite depth.
				 */
				throw log.infiniteRecursionForDerivedFrom( latestType, latestValuePath );
			}
		}
		else {
			initialNodeCollectingDependency = this;
		}

		if ( derivedFrom.isEmpty() ) {
			parentNode.parentNode().collectDependency( this.modelPathFromLastEntityNode );
		}
		else {
			/*
			 * The value represented by this node is derived from other, base values.
			 * If we rely on the value represented by this node when indexing,
			 * then we indirectly rely on these base values.
			 *
			 * We don't just call lastEntityNode.collectDependency() for each path to the base values,
			 * because the paths may cross the entity boundaries, meaning they may have a prefix
			 * leading to a different entity, and a suffix leading to the value we rely on.
			 * This means we must go through the dependency collector tree to properly resolve
			 * the entities that should trigger reindexing of our root entity when they change.
			 */
			PojoIndexingDependencyCollectorTypeNode<?> lastTypeNode = parentNode.parentNode();
			for ( PojoModelPathValueNode path : derivedFrom ) {
				PojoModelPathBinder.bind(
						lastTypeNode, path,
						PojoIndexingDependencyCollectorNode.walker( initialNodeCollectingDependency )
				);
			}
		}
	}

	@Override
	PojoIndexingDependencyCollectorTypeNode<?> lastEntityNode() {
		return parentNode.lastEntityNode();
	}

	@Override
	ReindexOnUpdate reindexOnUpdate() {
		return reindexOnUpdate;
	}

	@Override
	void markForReindexing(AbstractPojoImplicitReindexingResolverTypeNodeBuilder<?, ?> inverseSideEntityTypeNodeBuilder,
			BoundPojoModelPathValueNode<?, ?, ?> dependencyPathFromInverseSideEntityTypeNode) {
		PojoTypeModel<?> inverseSideEntityType = inverseSideEntityTypeNodeBuilder.getTypeModel();
		PojoRawTypeModel<?> inverseSideRawEntityType = inverseSideEntityType.rawType();
		PojoTypeModel<V> expectedInverseSideEntityType = modelPathFromLastEntityNode.type().getTypeModel();
		PojoRawTypeModel<?> expectedInverseSideEntityRawType = expectedInverseSideEntityType.rawType();
		if ( !inverseSideRawEntityType.isSubTypeOf( expectedInverseSideEntityRawType ) ) {
			throw new AssertionFailure(
					"Error while building the automatic reindexing resolver at path " + modelPathFromLastEntityNode
							+ ": the dependency collector was passed a resolver builder with incorrect type; "
							+ " got " + inverseSideRawEntityType + ", but a subtype of " + expectedInverseSideEntityRawType
							+ " was expected."
			);
		}

		Map<PojoRawTypeModel<?>, PojoModelPathValueNode> inverseAssociationsPaths =
				getInverseAssociationPathByConcreteEntityType( inverseSideEntityTypeNodeBuilder.getTypeModel() );
		for ( Map.Entry<PojoRawTypeModel<?>, PojoModelPathValueNode> entry : inverseAssociationsPaths.entrySet() ) {
			markForReindexingUsingAssociationInverseSideWithOriginalSideConcreteType(
					entry.getKey(), inverseSideEntityTypeNodeBuilder, entry.getValue(),
					dependencyPathFromInverseSideEntityTypeNode
			);
		}
	}

	private Map<PojoRawTypeModel<?>, PojoModelPathValueNode> getInverseAssociationPathByConcreteEntityType(
			PojoTypeModel<?> inverseSideEntityType) {
		PojoRawTypeModel<?> inverseSideRawEntityType = inverseSideEntityType.rawType();

		// Cache the inverse association path, as we may compute it many times, which may be costly
		Map<PojoRawTypeModel<?>, PojoModelPathValueNode> result = inverseAssociationPathCache.get( inverseSideRawEntityType );
		if ( result == null ) {
			if ( !inverseAssociationPathCache.containsKey( inverseSideRawEntityType ) ) {
				PojoTypeModel<?> originalSideEntityType = lastEntityNode().typeModel();
				PojoRawTypeModel<?> originalSideRawEntityType = originalSideEntityType.rawType();

				// Use a LinkedHashMap for deterministic iteration
				result = new LinkedHashMap<>();

				for ( PojoRawTypeModel<?> concreteEntityType :
						buildingHelper.getConcreteEntitySubTypesForEntitySuperType( originalSideRawEntityType ) ) {
					BoundPojoModelPathValueNode<?, ?, ?> modelPathFromConcreteEntitySubType =
							applyProcessingPathToSubType( concreteEntityType, modelPathFromLastEntityNode );
					PojoModelPathValueNode inverseAssociationPath = buildingHelper.pathInverter()
							.invertPath( inverseSideEntityType, modelPathFromConcreteEntitySubType )
							.orElse( null );
					if ( inverseAssociationPath == null ) {
						throw log.cannotInvertAssociationForReindexing(
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

	private void markForReindexingUsingAssociationInverseSideWithOriginalSideConcreteType(PojoTypeModel<?> originalSideConcreteEntityType,
			AbstractPojoImplicitReindexingResolverTypeNodeBuilder<?, ?> typeNodeBuilder,
			PojoModelPathValueNode inverseAssociationPath,
			BoundPojoModelPathValueNode<?, ?, ?> dependencyPathFromInverseSideEntityTypeNode) {
		PojoTypeModel<?> inverseSideEntityType = typeNodeBuilder.getTypeModel();
		PojoRawTypeModel<?> inverseSideRawEntityType = inverseSideEntityType.rawType();

		PojoRawTypeModel<?> originalSideRawConcreteEntityType = originalSideConcreteEntityType.rawType();

		// Attempt to apply the inverse association path to the given builder
		PojoImplicitReindexingResolverValueNodeBuilderDelegate<?> valueNodeBuilderDelegate;
		Set<? extends PojoRawTypeModel<?>> valueNodeTypeConcreteEntitySubTypes;
		try {
			valueNodeBuilderDelegate = PojoModelPathBinder.bind(
					typeNodeBuilder, inverseAssociationPath, PojoImplicitReindexingResolverBuilder.walker()
			);

			PojoRawTypeModel<?> inverseSideRawType = valueNodeBuilderDelegate.getTypeModel().rawType();
			valueNodeTypeConcreteEntitySubTypes = lastEntityNode().getConcreteEntitySubTypesForTypeToReindex(
					originalSideRawConcreteEntityType, inverseSideRawType
			);
		}
		// Note: this should catch errors related to properties not found, among others.
		catch (RuntimeException e) {
			throw log.cannotApplyImplicitInverseAssociationPath(
					inverseSideRawEntityType, inverseAssociationPath,
					originalSideRawConcreteEntityType, modelPathFromLastEntityNode.toUnboundPath(),
					e.getMessage(), e
			);
		}

		lastEntityNode().markForReindexing(
				valueNodeBuilderDelegate,
				valueNodeTypeConcreteEntitySubTypes,
				dependencyPathFromInverseSideEntityTypeNode
		);
	}

	private BoundPojoModelPathValueNode<?, ?, ?> applyProcessingPathToSubType(PojoRawTypeModel<?> rootSubType,
			BoundPojoModelPathValueNode<?, ?, ?> source) {
		return PojoModelPathBinder.bind(
				BoundPojoModelPath.root( rootSubType ),
				source.toUnboundPath(),
				BoundPojoModelPath.walker( buildingHelper.extractorBinder() )
		);
	}

}
