/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.path.impl;

import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

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

	// TODO HSEARCH-3318 This is an approximation, ideally we should pass a name AND access type
	public BoundPojoModelPathPropertyNode<T, ?> property(String propertyName) {
		PojoPropertyModel<?> propertyModel = getTypeModel().property( propertyName );
		return new BoundPojoModelPathPropertyNode<>(
				this, propertyModel
		);
	}

	@Override
	public abstract BoundPojoModelPathValueNode<?, ?, ?> getParent();

	@Override
	public PojoModelPathValueNode toUnboundPath() {
		PojoModelPath.Builder builder = PojoModelPath.builder();
		appendPath( builder );
		return builder.toValuePathOrNull();
	}

	/**
	 * @return A sibling path node representing this type, cast to the given type.
	 */
	public abstract <U> BoundPojoModelPathCastedTypeNode<?, ? extends U> castTo(PojoRawTypeModel<U> typeModel);

	@Override
	void appendSelfPath(PojoModelPath.Builder builder) {
		// Nothing to do
	}

	public abstract PojoTypeModel<T> getTypeModel();
}
