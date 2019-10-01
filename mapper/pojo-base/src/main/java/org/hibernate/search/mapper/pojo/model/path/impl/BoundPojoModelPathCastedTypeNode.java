/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.path.impl;

import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

/**
 * @param <T> The type represented by the parent node, whose values are casted to {@link U}.
 * @param <U> The type represented by this node.
 */
public class BoundPojoModelPathCastedTypeNode<T, U> extends BoundPojoModelPathTypeNode<U> {

	private final BoundPojoModelPathValueNode<?, ?, T> parent;
	private final PojoRawTypeModel<U> typeModel;

	BoundPojoModelPathCastedTypeNode(BoundPojoModelPathValueNode<?, ?, T> parent, PojoRawTypeModel<U> typeModel) {
		this.parent = parent;
		this.typeModel = typeModel;
	}

	@Override
	public BoundPojoModelPathValueNode<?, ?, T> getParent() {
		return parent;
	}

	@Override
	public PojoRawTypeModel<U> getTypeModel() {
		return typeModel;
	}

	@Override
	void appendSelfPath(StringBuilder builder) {
		builder.append( "casted type " ).append( getTypeModel() );
	}
}
