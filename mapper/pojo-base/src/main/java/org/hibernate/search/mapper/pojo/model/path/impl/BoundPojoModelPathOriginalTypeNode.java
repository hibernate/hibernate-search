/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.path.impl;

import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

/**
 * @param <T> The type represented by this node.
 */
public class BoundPojoModelPathOriginalTypeNode<T> extends BoundPojoModelPathTypeNode<T> {

	private final BoundPojoModelPathValueNode<?, ?, T> parent;
	private final PojoTypeModel<T> typeModel;

	BoundPojoModelPathOriginalTypeNode(BoundPojoModelPathValueNode<?, ?, T> parent, PojoTypeModel<T> typeModel) {
		this.parent = parent;
		this.typeModel = typeModel;
	}

	@Override
	public BoundPojoModelPathValueNode<?, ?, T> getParent() {
		return parent;
	}

	@Override
	public PojoTypeModel<T> getTypeModel() {
		return typeModel;
	}

	@Override
	public <U> BoundPojoModelPathCastedTypeNode<T, ? extends U> castTo(PojoRawTypeModel<U> typeModel) {
		return new BoundPojoModelPathCastedTypeNode<>( getParent(), typeModel.cast( getTypeModel() ) );
	}

	@Override
	void appendSelfPath(StringBuilder builder) {
		builder.append( "type " ).append( getTypeModel() );
	}
}
