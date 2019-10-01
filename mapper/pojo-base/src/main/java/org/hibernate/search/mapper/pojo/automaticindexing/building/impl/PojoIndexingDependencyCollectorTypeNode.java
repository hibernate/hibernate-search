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
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A node representing a type in a dependency collector.
 *
 * @see PojoIndexingDependencyCollectorValueNode
 *
 * @param <T> The represented type
 */
public class PojoIndexingDependencyCollectorTypeNode<T> extends PojoIndexingDependencyCollectorNode {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final AbstractPojoIndexingDependencyCollectorValueNode parentNode;
	/**
	 * The path to this node from this node, i.e. a root to be used to build model paths for child nodes.
	 */
	private final BoundPojoModelPathTypeNode<T> modelPathFromCurrentNode;
	/**
	 * The last entity node among the ancestor nodes,
	 * i.e. the closest type node representing an entity type.
	 */
	private final PojoIndexingDependencyCollectorTypeNode<?> lastEntityNode;
	private final BoundPojoModelPathTypeNode<T> modelPathFromLastEntityNode;

	PojoIndexingDependencyCollectorTypeNode(PojoRawTypeModel<T> typeModel,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		super( buildingHelper );
		this.parentNode = null;
		this.modelPathFromCurrentNode = BoundPojoModelPath.root( typeModel );
		this.lastEntityNode = this;
		this.modelPathFromLastEntityNode = modelPathFromCurrentNode;
	}

	PojoIndexingDependencyCollectorTypeNode(PojoIndexingDependencyCollectorValueNode<?, T> parentNode,
			PojoIndexingDependencyCollectorTypeNode<?> lastEntityNode,
			BoundPojoModelPathTypeNode<T> modelPathFromLastEntityNode,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		super( buildingHelper );
		this.parentNode = parentNode;
		PojoTypeModel<T> typeModel = modelPathFromLastEntityNode.getTypeModel();
		this.modelPathFromCurrentNode = BoundPojoModelPath.root( typeModel );
		if ( buildingHelper.isEntity( typeModel.getRawType() ) ) {
			this.lastEntityNode = this;
			this.modelPathFromLastEntityNode = modelPathFromCurrentNode;
		}
		else {
			this.lastEntityNode = lastEntityNode;
			this.modelPathFromLastEntityNode = modelPathFromLastEntityNode;
		}
	}

	PojoIndexingDependencyCollectorTypeNode(PojoIndexingDependencyCollectorDisjointValueNode<T> parentNode,
			PojoRawTypeModel<T> typeModel,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		super( buildingHelper );
		this.parentNode = parentNode;
		this.modelPathFromCurrentNode = BoundPojoModelPath.root( typeModel );
		this.lastEntityNode = this;
		this.modelPathFromLastEntityNode = modelPathFromCurrentNode;
	}

	/*
	 * modelPathFromCurrentNode, modelPathFromRootEntityNode and modelPathFromLastEntityNode
	 * reference the same type, just from a different root.
	 * Thus fetching the same property results in the same property type.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public PojoIndexingDependencyCollectorPropertyNode<T, ?> property(String propertyName) {
		return new PojoIndexingDependencyCollectorPropertyNode<>(
				this,
				(BoundPojoModelPathPropertyNode) modelPathFromCurrentNode.property( propertyName ),
				lastEntityNode,
				(BoundPojoModelPathPropertyNode) modelPathFromLastEntityNode.property( propertyName ),
				buildingHelper
		);
	}

	public PojoIndexingDependencyCollectorDisjointValueNode<?> disjointValue(
			BoundPojoModelPathValueNode<?, ?, ?> inverseAssociationPath) {
		if ( lastEntityNode != this ) {
			throw new AssertionFailure( "disjointValue() called on a non-entity node" );
		}

		PojoRawTypeModel<?> inverseSideEntityTypeModel = inverseAssociationPath.getRootType().getRawType();
		if ( !buildingHelper.isEntity( inverseAssociationPath.getRootType().getRawType() ) ) {
			throw new AssertionFailure(
					"Encountered a type node whose parent is a disjoint value node, but does not represent an entity type?"
			);
		}

		return new PojoIndexingDependencyCollectorDisjointValueNode<>(
				this,
				inverseSideEntityTypeModel,
				inverseAssociationPath,
				buildingHelper
		);
	}

	@Override
	ReindexOnUpdate getReindexOnUpdate() {
		return parentNode == null ? ReindexOnUpdate.DEFAULT : parentNode.getReindexOnUpdate();
	}

	void collectDependency(BoundPojoModelPathValueNode<?, ?, ?> dirtyPathFromEntityType) {
		if ( lastEntityNode != this ) {
			throw new AssertionFailure( "collectDependency() called on a non-entity node" );
		}
		PojoRawTypeModel<? super T> rawType = modelPathFromLastEntityNode.getTypeModel().getRawType();
		if ( parentNode == null ) {
			/*
			 * This node represents an indexed entity (A).
			 * The current method being called means that entity A uses some value
			 * that is directly part of entity A (not retrieved through associations) when it is indexed.
			 * The value used during indexing is represented by "dirtyPathFromEntityType".
			 * Thus we must make sure that whenever "dirtyPathFromEntityType" changes in entity A,
			 * entity A gets reindexed.
			 * This is what the calls below achieve.
			 */
			PojoImplicitReindexingResolverBuilder<?> builder =
					buildingHelper.getOrCreateResolverBuilder( rawType );
			/*
			 * Note that, on contrary to the "else" branch, we don't need to loop on the builder corresponding
			 * to each entity subtype.
			 * This is because one dependency collectors will be created for each indexed entity type,
			 * so the current method will already be called for each relevant entity subtype
			 * (i.e. the entity subtype that are also indexed, which may not be all of then).
			 */
			builder.addDirtyPathTriggeringSelfReindexing( dirtyPathFromEntityType );
		}
		else {
			/*
			 * This node represents an entity (B) referenced from another, indexed entity (A).
			 * Also, the current method being called means that entity A uses some value
			 * from entity B when it is indexed.
			 * The value used during indexing is represented by "dirtyPathFromEntityType".
			 * Thus we must make sure that whenever "dirtyPathFromEntityType" changes in entity B,
			 * entity A gets reindexed.
			 * This is what the calls below achieve.
			 */
			for ( PojoRawTypeModel<?> concreteEntityType :
					buildingHelper.getConcreteEntitySubTypesForEntitySuperType( rawType ) ) {
				PojoImplicitReindexingResolverOriginalTypeNodeBuilder<?> builder =
						buildingHelper.getOrCreateResolverBuilder( concreteEntityType )
								.containingEntitiesResolverRoot();
				parentNode.markForReindexing( builder, dirtyPathFromEntityType );
			}
		}
	}

	void markForReindexing(PojoImplicitReindexingResolverValueNodeBuilderDelegate<?> valueNodeBuilderDelegate,
			Set<? extends PojoRawTypeModel<?>> valueNodeTypeConcreteEntitySubTypes,
			BoundPojoModelPathValueNode<?, ?, ?> dependencyPathFromInverseSideEntityTypeNode) {
		if ( lastEntityNode != this ) {
			throw new AssertionFailure( "markForReindexing() called on a non-entity node" );
		}

		if ( parentNode != null ) {
			/*
			 * This type is not the indexed type.
			 * Continue to build the inverse path from the "potentially dirty" value to the indexed type.
			 */
			for ( PojoRawTypeModel<?> concreteEntityType : valueNodeTypeConcreteEntitySubTypes ) {
				AbstractPojoImplicitReindexingResolverTypeNodeBuilder<?, ?> inverseValueTypeBuilder =
						valueNodeBuilderDelegate.type( concreteEntityType );
				parentNode.markForReindexing(
						inverseValueTypeBuilder, dependencyPathFromInverseSideEntityTypeNode
				);
			}
		}
		else {
			/*
			 * This type *is* the indexed type.
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

	/*
	 * The entities to reindex will always be instances of both the entity type on the original side
	 * (because that's the one we want to reindex)
	 * and the type targeted by the inverse side of the association
	 * (because that's all we will ever retrieve at runtime).
	 * Thus we will only consider the most specific type of the two when resolving entities to reindex.
	 */
	Set<? extends PojoRawTypeModel<?>> getConcreteEntitySubTypesForTypeToReindex(
			PojoRawTypeModel<?> originalSideRawType, PojoRawTypeModel<?> inverseSideRawType) {
		if ( inverseSideRawType.isSubTypeOf( originalSideRawType ) ) {
			return buildingHelper.getConcreteEntitySubTypesForEntitySuperType( inverseSideRawType );
		}
		else if ( originalSideRawType.isSubTypeOf( inverseSideRawType ) ) {
			return buildingHelper.getConcreteEntitySubTypesForEntitySuperType( originalSideRawType );
		}
		else {
			throw log.incorrectTargetTypeForInverseAssociation( inverseSideRawType, originalSideRawType );
		}
	}

	PojoTypeModel<T> getTypeModel() {
		return modelPathFromLastEntityNode.getTypeModel();
	}
}
