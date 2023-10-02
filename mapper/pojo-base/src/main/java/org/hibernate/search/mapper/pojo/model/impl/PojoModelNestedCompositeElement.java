/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.impl;

import java.util.Collection;

import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.AbstractPojoIndexingDependencyCollectorDirectValueNode;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoIndexingDependencyCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoIndexingDependencyCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.PojoElementAccessor;
import org.hibernate.search.mapper.pojo.model.PojoModelProperty;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoPropertyAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

/**
 * @param <T> The type holding the property.
 * @param <P> The type of the property.
 */
class PojoModelNestedCompositeElement<T, P> extends AbstractPojoModelCompositeElement<P> implements PojoModelProperty {

	private final AbstractPojoModelCompositeElement<T> parent;
	private final BoundPojoModelPathValueNode<T, P, P> modelPath;
	private final PojoPropertyAdditionalMetadata propertyAdditionalMetadata;

	PojoModelNestedCompositeElement(AbstractPojoModelCompositeElement<T> parent,
			BoundPojoModelPathPropertyNode<T, P> modelPath,
			PojoPropertyAdditionalMetadata propertyAdditionalMetadata) {
		super( parent );
		this.parent = parent;
		this.modelPath = modelPath.valueWithoutExtractors();
		this.propertyAdditionalMetadata = propertyAdditionalMetadata;
	}

	@Override
	public <M> Collection<M> markers(Class<M> markerType) {
		return propertyAdditionalMetadata.getMarkers( markerType );
	}

	@Override
	public String name() {
		return modelPath.getParent().getPropertyModel().name();
	}

	public void contributeDependencies(PojoIndexingDependencyCollectorTypeNode<T> dependencyCollector) {
		if ( hasAccessor() ) {
			@SuppressWarnings("unchecked") // We used the same property as in modelPath, on the same type. The result must have the same type.
			PojoIndexingDependencyCollectorPropertyNode<T, P> collectorPropertyNode =
					(PojoIndexingDependencyCollectorPropertyNode<T, P>) dependencyCollector.property( name() );
			AbstractPojoIndexingDependencyCollectorDirectValueNode<P, P> collectorValueNode =
					collectorPropertyNode.value( modelPath.getBoundExtractorPath() );
			collectorValueNode.collectDependency();
			contributePropertyDependencies( collectorValueNode.type() );
		}
	}

	@Override
	PojoElementAccessor<P> doCreateAccessor() {
		return new PojoPropertyElementAccessor<>( parent.createAccessor(), getHandle(), modelPath.toUnboundPath() );
	}

	@Override
	BoundPojoModelPathTypeNode<P> getModelPathTypeNode() {
		return modelPath.type();
	}

	ValueReadHandle<P> getHandle() {
		return modelPath.getParent().getPropertyModel().handle();
	}
}
