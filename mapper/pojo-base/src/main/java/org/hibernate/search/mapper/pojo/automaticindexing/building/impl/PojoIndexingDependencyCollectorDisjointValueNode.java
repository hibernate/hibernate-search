/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.building.impl;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoModelPathBinder;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A node representing a disjoint value in a dependency collector,
 * i.e. a value whose path from the parent node is unknown.
 * <p>
 * This is useful for bridges that need to declare reindexing
 * without being able to specify which path they use exactly.
 *
 * @param <V> The extracted value type
 */
public class PojoIndexingDependencyCollectorDisjointValueNode<V>
		extends AbstractPojoIndexingDependencyCollectorValueNode {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoIndexingDependencyCollectorTypeNode<?> parentNode;
	private final PojoRawTypeModel<V> inverseSideEntityTypeModel;
	private final BoundPojoModelPathValueNode<?, ?, ?> inverseAssociationPath;

	PojoIndexingDependencyCollectorDisjointValueNode(PojoIndexingDependencyCollectorTypeNode<?> parentNode,
			PojoRawTypeModel<V> inverseSideEntityTypeModel,
			BoundPojoModelPathValueNode<?, ?, ?> inverseAssociationPath,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		super( buildingHelper );
		this.parentNode = parentNode;
		this.inverseSideEntityTypeModel = inverseSideEntityTypeModel;
		this.inverseAssociationPath = inverseAssociationPath;
		if ( !buildingHelper.isEntity( inverseSideEntityTypeModel ) ) {
			throw new AssertionFailure(
					"Encountered a type node whose parent is a disjoint value node, but does not represent an entity type?"
			);
		}
		if ( !inverseAssociationPath.getRootType().equals( inverseSideEntityTypeModel ) ) {
			throw new AssertionFailure(
					"Inconsistent root type for " + inverseAssociationPath + "; expected " + inverseSideEntityTypeModel
			);
		}
	}

	@Override
	public PojoIndexingDependencyCollectorTypeNode<?> type() {
		return new PojoIndexingDependencyCollectorTypeNode<>( this,
				BoundPojoModelPath.root( inverseSideEntityTypeModel ), buildingHelper );
	}

	@Override
	PojoIndexingDependencyCollectorTypeNode<?> lastEntityNode() {
		return parentNode.lastEntityNode();
	}

	@Override
	ReindexOnUpdate reindexOnUpdate() {
		return parentNode.reindexOnUpdate();
	}

	@Override
	void collectDependency(BoundPojoModelPathValueNode<?, ?, ?> dirtyPathFromEntityType) {
		parentNode.collectDependency( dirtyPathFromEntityType );
	}

	@Override
	void markForReindexing(AbstractPojoImplicitReindexingResolverTypeNodeBuilder<?, ?> inverseSideEntityTypeNodeBuilder,
			BoundPojoModelPathValueNode<?, ?, ?> dependencyPathFromInverseSideEntityTypeNode) {
		PojoTypeModel<?> inverseSideEntityType = inverseSideEntityTypeNodeBuilder.getTypeModel();
		PojoRawTypeModel<?> inverseSideRawEntityType = inverseSideEntityType.rawType();
		PojoRawTypeModel<?> originalSideRawConcreteEntityType = parentNode.typeModel().rawType();

		PojoModelPathValueNode inverseAssociationUnboundPath = inverseAssociationPath.toUnboundPath();

		/*
		 * This node represents an entity (B) with an association to another entity (A), modeled by "inverseAssociationPath".
		 * Also, the current method being called means that a bridged applied to entity A uses some value
		 * from entity B when it is indexed. We don't know how the bridge accesses B,
		 * but we now the inverse side of that association is "inverseAssociationPath".
		 * The value from entity B used during indexing is represented by "dependencyPathFromInverseSideEntityTypeNode".
		 * Thus we must make sure that whenever "dependencyPathFromInverseSideEntityTypeNode" changes in entity B,
		 * entity A (or its containing indexed entity) gets reindexed.
		 * This is what the calls below achieve.
		 */

		// Attempt to apply the inverse path to the given builder
		PojoImplicitReindexingResolverValueNodeBuilderDelegate<?> valueNodeBuilderDelegate;
		Set<? extends PojoRawTypeModel<?>> valueNodeTypeConcreteEntitySubTypes;
		try {
			valueNodeBuilderDelegate = PojoModelPathBinder.bind(
					inverseSideEntityTypeNodeBuilder, inverseAssociationUnboundPath,
					PojoImplicitReindexingResolverBuilder.walker()
			);

			PojoRawTypeModel<?> inverseSideRawType = valueNodeBuilderDelegate.getTypeModel().rawType();
			valueNodeTypeConcreteEntitySubTypes = parentNode.getConcreteEntitySubTypesForTypeToReindex(
					originalSideRawConcreteEntityType, inverseSideRawType
			);
		}
		// Note: this should catch errors related to properties not found, among others.
		catch (RuntimeException e) {
			throw log.cannotApplyExplicitInverseAssociationPath(
					inverseSideRawEntityType, inverseAssociationUnboundPath,
					originalSideRawConcreteEntityType,
					e.getMessage(), e
			);
		}

		parentNode.markForReindexing(
				valueNodeBuilderDelegate,
				valueNodeTypeConcreteEntitySubTypes,
				dependencyPathFromInverseSideEntityTypeNode
		);
	}

}
