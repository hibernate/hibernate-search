/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.dirtiness.building.impl;

import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;

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

	PojoIndexingDependencyCollectorTypeNode(PojoRawTypeModel<T> typeModel,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		this( null, BoundPojoModelPath.root( typeModel ), buildingHelper );
	}

	PojoIndexingDependencyCollectorTypeNode(PojoIndexingDependencyCollectorValueNode<?, T> parent,
			BoundPojoModelPathTypeNode<T> modelPath,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		super( buildingHelper );
		this.parent = parent;
		this.modelPath = modelPath;
	}

	public PojoIndexingDependencyCollectorPropertyNode<T, ?> property(PropertyHandle propertyHandle) {
		return new PojoIndexingDependencyCollectorPropertyNode<>(
				this, modelPath.property( propertyHandle ), buildingHelper
		);
	}

	void collectDependency() {
		if ( parent != null ) {
			// FIXME handle the case when this type is not an entity type (the Set below will be empty)
			PojoRawTypeModel<? super T> rawType = modelPath.getTypeModel().getRawType();
			for ( PojoRawTypeModel<?> concreteEntityType :
					buildingHelper.getConcreteEntitySubTypesForEntitySuperType( rawType ) ) {
				PojoImplicitReindexingResolverTypeNodeBuilder<?> builder =
						buildingHelper.getOrCreateResolverBuilder( concreteEntityType );
				parent.markForReindexing( builder );
			}
		}
	}

	PojoIndexingDependencyCollectorValueNode<?, T> getParent() {
		return parent;
	}

}
