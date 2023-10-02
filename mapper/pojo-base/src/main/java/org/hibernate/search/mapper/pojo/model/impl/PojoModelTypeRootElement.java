/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.impl;

import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoIndexingDependencyCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.PojoElementAccessor;
import org.hibernate.search.mapper.pojo.model.PojoModelType;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl.PojoTypeAdditionalMetadataProvider;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;

/**
 * @param <T> The type used as a root element.
 */
public class PojoModelTypeRootElement<T> extends AbstractPojoModelCompositeElement<T> implements PojoModelType {

	private final BoundPojoModelPathTypeNode<T> modelPath;

	public PojoModelTypeRootElement(BoundPojoModelPathTypeNode<T> modelPath,
			PojoBootstrapIntrospector introspector,
			PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider) {
		super( introspector, typeAdditionalMetadataProvider );
		this.modelPath = modelPath;
	}

	@Override
	public String toString() {
		return modelPath.getTypeModel().toString();
	}

	public void contributeDependencies(PojoIndexingDependencyCollectorTypeNode<T> dependencyCollector) {
		contributePropertyDependencies( dependencyCollector );
	}

	@Override
	PojoElementAccessor<T> doCreateAccessor() {
		return new PojoRootElementAccessor<>();
	}

	@Override
	BoundPojoModelPathTypeNode<T> getModelPathTypeNode() {
		return modelPath;
	}
}
