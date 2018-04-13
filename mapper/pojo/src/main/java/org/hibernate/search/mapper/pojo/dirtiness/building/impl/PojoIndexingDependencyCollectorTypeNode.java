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
				modelPathFromRootEntityNode.property( propertyHandle ),
				lastEntityNode,
				(BoundPojoModelPathPropertyNode) modelPathFromLastEntityNode.property( propertyHandle ),
				buildingHelper
		);
	}

	void collectDependency() {
		if ( lastEntityNode != this ) {
			throw new AssertionFailure( "collectDependency() called on a non-entity node" );
		}
		if ( parentNode != null ) {
			// This node has an entity type: mark the root for reindexing whenever this node is changed.
			PojoRawTypeModel<? super T> rawType = modelPathFromRootEntityNode.getTypeModel().getRawType();
			for ( PojoRawTypeModel<?> concreteEntityType :
					buildingHelper.getConcreteEntitySubTypesForEntitySuperType( rawType ) ) {
				PojoImplicitReindexingResolverOriginalTypeNodeBuilder<?> builder =
						buildingHelper.getOrCreateResolverBuilder( concreteEntityType );
				parentNode.markForReindexing( builder );
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
