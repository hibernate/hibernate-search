/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.dirtiness.building.impl;

import org.hibernate.search.mapper.pojo.dirtiness.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.util.AssertionFailure;

/**
 * A node representing a type in a dependency collector.
 *
 * @see PojoIndexingDependencyCollectorValueNode
 *
 * @param <T> The represented type
 */
public class PojoIndexingDependencyCollectorTypeNode<T> extends AbstractPojoIndexingDependencyCollectorNode {

	private final PojoIndexingDependencyCollectorValueNode<?, T> parentNode;
	private final BoundPojoModelPathTypeNode<T> modelPathFromRootEntityNode;
	private final PojoIndexingDependencyCollectorTypeNode<?> lastEntityNode;
	private final BoundPojoModelPathTypeNode<T> modelPathFromLastEntityNode;

	PojoIndexingDependencyCollectorTypeNode(PojoRawTypeModel<T> typeModel,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		super( buildingHelper );
		this.parentNode = null;
		this.modelPathFromRootEntityNode = BoundPojoModelPath.root( typeModel );
		this.lastEntityNode = this;
		this.modelPathFromLastEntityNode = modelPathFromRootEntityNode;
	}

	PojoIndexingDependencyCollectorTypeNode(PojoIndexingDependencyCollectorValueNode<?, T> parentNode,
			BoundPojoModelPathTypeNode<T> modelPathFromRootEntityNode,
			PojoIndexingDependencyCollectorTypeNode<?> lastEntityNode,
			BoundPojoModelPathTypeNode<T> modelPathFromLastEntityNode,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		super( buildingHelper );
		this.parentNode = parentNode;
		this.modelPathFromRootEntityNode = modelPathFromRootEntityNode;
		if ( buildingHelper.isEntity( modelPathFromRootEntityNode.getTypeModel().getRawType() ) ) {
			this.lastEntityNode = this;
			this.modelPathFromLastEntityNode = BoundPojoModelPath.root( modelPathFromRootEntityNode.getTypeModel() );
		}
		else {
			this.lastEntityNode = lastEntityNode;
			this.modelPathFromLastEntityNode = modelPathFromLastEntityNode;
		}
	}

	/*
	 * modelPathFromRootEntityNode and modelPathFromLastEntityNode reference the same type, just from a different root.
	 * Thus applying the same property handle results in the same property type.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public PojoIndexingDependencyCollectorPropertyNode<T, ?> property(PropertyHandle propertyHandle) {
		return new PojoIndexingDependencyCollectorPropertyNode<>(
				this,
				modelPathFromRootEntityNode.property( propertyHandle ),
				lastEntityNode,
				(BoundPojoModelPathPropertyNode) modelPathFromLastEntityNode.property( propertyHandle ),
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
		PojoRawTypeModel<? super T> rawType = modelPathFromRootEntityNode.getTypeModel().getRawType();
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

	PojoIndexingDependencyCollectorValueNode<?, T> getParentNode() {
		return parentNode;
	}

	PojoTypeModel<T> getTypeModel() {
		return modelPathFromRootEntityNode.getTypeModel();
	}

}
