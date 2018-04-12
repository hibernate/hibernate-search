/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.path.impl;

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
	void appendSelfPath(StringBuilder builder) {
		builder.append( "type " ).append( getTypeModel() );
	}
}
