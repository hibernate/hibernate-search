/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.path.impl;

import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerExtractorPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

/**
 * @param <T> The property holder type of this node, i.e. the type from which the property is retrieved.
 * @param <P> The type of the property represented by this node.
 */
public class BoundPojoModelPathPropertyNode<T, P> extends BoundPojoModelPath {

	private final BoundPojoModelPathTypeNode<T> parent;
	private final PojoPropertyModel<P> propertyModel;

	BoundPojoModelPathPropertyNode(BoundPojoModelPathTypeNode<T> parent, PojoPropertyModel<P> propertyModel) {
		this.parent = parent;
		this.propertyModel = propertyModel;
	}

	@Override
	public BoundPojoModelPathTypeNode<T> getParent() {
		return parent;
	}

	@Override
	public PojoTypeModel<?> getRootType() {
		return parent.getRootType();
	}

	@Override
	public PojoModelPathPropertyNode toUnboundPath() {
		PojoModelPath.Builder builder = PojoModelPath.builder();
		appendPath( builder );
		return builder.toPropertyPathOrNull();
	}

	public BoundPojoModelPathValueNode<T, P, P> valueWithoutExtractors() {
		return value( BoundContainerExtractorPath.noExtractors( propertyModel.typeModel() ) );
	}

	public <V> BoundPojoModelPathValueNode<T, P, V> value(BoundContainerExtractorPath<? super P, V> extractorPath) {
		return new BoundPojoModelPathValueNode<>( this, extractorPath );
	}

	public PojoPropertyModel<P> getPropertyModel() {
		return propertyModel;
	}

	@Override
	void appendSelfPath(StringBuilder builder) {
		builder.append( "." ).append( propertyModel.name() );
	}

	@Override
	void appendSelfPath(PojoModelPath.Builder builder) {
		builder.property( propertyModel.name() );
	}
}
