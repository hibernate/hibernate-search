/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.path.impl;

import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
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

	@SuppressWarnings("unchecked") // TODO HSEARCH-3318 This is an approximation, ideally we should not pass a propertyHandle but a name and access type
	public <P> BoundPojoModelPathPropertyNode<T, P> property(PropertyHandle<P> propertyHandle) {
		return new BoundPojoModelPathPropertyNode<>(
				this, propertyHandle,
				(PojoPropertyModel<P>) getTypeModel().getProperty( propertyHandle.getName() )
		);
	}

	@Override
	public abstract BoundPojoModelPathValueNode<?, ?, ?> getParent();

	@Override
	public PojoModelPathValueNode toUnboundPath() {
		BoundPojoModelPathValueNode<?, ?, ?> parent = getParent();
		if ( parent == null ) {
			return null;
		}
		else {
			return parent.toUnboundPath();
		}
	}

	public abstract PojoTypeModel<T> getTypeModel();
}
