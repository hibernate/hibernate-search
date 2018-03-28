/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.path.impl;

import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;

/**
 * @param <T> The property holder type of this node, i.e. the type from which the property is retrieved.
 * @param <P> The type of the property represented by this node.
 */
public class BoundPojoModelPathPropertyNode<T, P> extends BoundPojoModelPath {

	private final BoundPojoModelPathTypeNode<T> parent;
	private final PropertyHandle propertyHandle;
	private final PojoPropertyModel<P> propertyModel;

	BoundPojoModelPathPropertyNode(BoundPojoModelPathTypeNode<T> parent, PropertyHandle propertyHandle,
			PojoPropertyModel<P> propertyModel) {
		this.parent = parent;
		this.propertyHandle = propertyHandle;
		this.propertyModel = propertyModel;
	}

	@Override
	public BoundPojoModelPathTypeNode<T> parent() {
		return parent;
	}

	public <V> BoundPojoModelPathValueNode<T, P, V> value(BoundContainerValueExtractorPath<P, V> extractorPath) {
		return new BoundPojoModelPathValueNode<>( this, extractorPath );
	}

	public PropertyHandle getPropertyHandle() {
		return propertyHandle;
	}

	public PojoPropertyModel<P> getPropertyModel() {
		return propertyModel;
	}

	public PojoModelPathPropertyNode toUnboundPath() {
		String propertyName = propertyHandle.getName();
		BoundPojoModelPathValueNode<?, ?, ?> parentParent = parent.parent();
		if ( parentParent == null ) {
			return PojoModelPath.fromRoot( propertyName );
		}
		else {
			return parentParent.toUnboundPath().property( propertyName );
		}
	}

	@Override
	void appendSelfPath(StringBuilder builder) {
		builder.append( "." ).append( propertyHandle.getName() );
	}
}
