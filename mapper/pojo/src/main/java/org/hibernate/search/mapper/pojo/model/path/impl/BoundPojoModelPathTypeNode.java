/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.path.impl;

import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;

/**
 * @param <T> The type represented by this node.
 */
public class BoundPojoModelPathTypeNode<T> extends BoundPojoModelPath {

	private final BoundPojoModelPathValueNode<?, ?, T> parent;
	private final PojoTypeModel<T> typeModel;

	BoundPojoModelPathTypeNode(BoundPojoModelPathValueNode<?, ?, T> parent, PojoTypeModel<T> typeModel) {
		this.parent = parent;
		this.typeModel = typeModel;
	}

	@Override
	public BoundPojoModelPathValueNode<?, ?, T> parent() {
		return parent;
	}

	public BoundPojoModelPathPropertyNode<T, ?> property(PropertyHandle propertyHandle) {
		return new BoundPojoModelPathPropertyNode<>(
				this, propertyHandle, typeModel.getProperty( propertyHandle.getName() )
		);
	}

	public PojoTypeModel<T> getTypeModel() {
		return typeModel;
	}

	@Override
	void appendSelfPath(StringBuilder builder) {
		builder.append( "type " ).append( typeModel );
	}
}
