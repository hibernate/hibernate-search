/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.identity.impl;

import java.util.Map;

import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingHelper;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexMappingCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;

class IdentityMappingCollectorTypeNode<T> extends AbstractIdentityMappingCollectorNode
		implements PojoIndexMappingCollectorTypeNode {

	private final BoundPojoModelPathTypeNode<T> modelPath;
	private final PojoIdentityMappingCollector identityMappingCollector;

	IdentityMappingCollectorTypeNode(BoundPojoModelPathTypeNode<T> modelPath,
			PojoMappingHelper mappingHelper,
			PojoIdentityMappingCollector identityMappingCollector) {
		super( mappingHelper );
		this.modelPath = modelPath;
		this.identityMappingCollector = identityMappingCollector;
	}

	@Override
	BoundPojoModelPathTypeNode<T> getModelPath() {
		return modelPath;
	}

	@Override
	public void typeBinder(TypeBinder builder, Map<String, Object> params) {
		// No-op, we're just collecting the identity mapping.
	}

	@Override
	public PojoIndexMappingCollectorPropertyNode property(String propertyName) {
		return new IdentityMappingCollectorPropertyNode<>( modelPath.property( propertyName ),
				mappingHelper, identityMappingCollector
		);
	}

}
