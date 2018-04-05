/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.dirtiness.building.impl;

import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
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

	private final PojoIndexingDependencyCollectorValueNode<?, T> parent;
	private final BoundPojoModelPathTypeNode<T> modelPath;
	private final PojoIndexingDependencyCollectorTypeNode<?> entityAncestor;
	private final BoundPojoModelPathTypeNode<T> modelPathFromEntity;

	PojoIndexingDependencyCollectorTypeNode(PojoRawTypeModel<T> typeModel,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		super( buildingHelper );
		this.parent = null;
		this.modelPath = BoundPojoModelPath.root( typeModel );
		this.entityAncestor = this;
		this.modelPathFromEntity = modelPath;
	}

	PojoIndexingDependencyCollectorTypeNode(PojoIndexingDependencyCollectorValueNode<?, T> parent,
			BoundPojoModelPathTypeNode<T> modelPath,
			PojoIndexingDependencyCollectorTypeNode<?> entityAncestor,
			BoundPojoModelPathTypeNode<T> modelPathFromEntity,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		super( buildingHelper );
		this.parent = parent;
		this.modelPath = modelPath;
		if ( buildingHelper.isEntity( modelPath.getTypeModel().getRawType() ) ) {
			this.entityAncestor = this;
			this.modelPathFromEntity = BoundPojoModelPath.root( modelPath.getTypeModel() );
		}
		else {
			this.entityAncestor = entityAncestor;
			this.modelPathFromEntity = modelPathFromEntity;
		}
	}

	/*
	 * modelPath and modelPathFromEntity represent the same type,
	 * so applying the same property handle results in the same property type.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public PojoIndexingDependencyCollectorPropertyNode<T, ?> property(PropertyHandle propertyHandle) {
		return new PojoIndexingDependencyCollectorPropertyNode<>(
				modelPath.property( propertyHandle ),
				entityAncestor,
				(BoundPojoModelPathPropertyNode) modelPathFromEntity.property( propertyHandle ),
				buildingHelper
		);
	}

	void collectDependency() {
		if ( entityAncestor != this ) {
			throw new AssertionFailure( "collectDependency() called on a non-entity node" );
		}
		if ( parent != null ) {
			// This node has an entity type: mark the root for reindexing whenever this node is changed.
			PojoRawTypeModel<? super T> rawType = modelPath.getTypeModel().getRawType();
			for ( PojoRawTypeModel<?> concreteEntityType :
					buildingHelper.getConcreteEntitySubTypesForEntitySuperType( rawType ) ) {
				PojoImplicitReindexingResolverOriginalTypeNodeBuilder<?> builder =
						buildingHelper.getOrCreateResolverBuilder( concreteEntityType );
				parent.markForReindexing( builder );
			}
		}
	}

	PojoIndexingDependencyCollectorValueNode<?, T> getParent() {
		return parent;
	}

	PojoTypeModel<T> getTypeModel() {
		return modelPath.getTypeModel();
	}

}
