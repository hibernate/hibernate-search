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
public abstract class BoundPojoModelPathTypeNode<T> extends BoundPojoModelPath {

	BoundPojoModelPathTypeNode() {
	}

	@Override
	public PojoTypeModel<?> getRootType() {
		BoundPojoModelPathValueNode<?, ?, ?> parent = getParent();
		if ( parent == null ) {
			return getTypeModel();
		}
		else {
			return parent.getRootType();
		}
	}

	public BoundPojoModelPathPropertyNode<T, ?> property(PropertyHandle propertyHandle) {
		return new BoundPojoModelPathPropertyNode<>(
				this, propertyHandle, getTypeModel().getProperty( propertyHandle.getName() )
		);
	}

	@Override
	public abstract BoundPojoModelPathValueNode<?, ?, ?> getParent();

	public abstract PojoTypeModel<T> getTypeModel();
}
