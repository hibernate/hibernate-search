/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.path.impl;

import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

/**
 * @param <T> The type represented by the parent node, whose values are casted to {@link U}.
 * @param <U> The type represented by this node.
 */
public class BoundPojoModelPathCastedTypeNode<T, U> extends BoundPojoModelPathTypeNode<U> {

	private final BoundPojoModelPathValueNode<?, ?, T> parent;
	private final PojoTypeModel<U> typeModel;

	BoundPojoModelPathCastedTypeNode(BoundPojoModelPathValueNode<?, ?, T> parent, PojoTypeModel<U> typeModel) {
		this.parent = parent;
		this.typeModel = typeModel;
	}

	@Override
	public BoundPojoModelPathValueNode<?, ?, T> getParent() {
		return parent;
	}

	@Override
	public PojoTypeModel<U> getTypeModel() {
		return typeModel;
	}

	@Override
	public <U2> BoundPojoModelPathCastedTypeNode<T, ? extends U2> castTo(PojoRawTypeModel<U2> typeModel) {
		return new BoundPojoModelPathCastedTypeNode<>( getParent(), typeModel.cast( getTypeModel() ) );
	}

	@Override
	void appendSelfPath(StringBuilder builder) {
		builder.append( "casted type " ).append( getTypeModel() );
	}
}
