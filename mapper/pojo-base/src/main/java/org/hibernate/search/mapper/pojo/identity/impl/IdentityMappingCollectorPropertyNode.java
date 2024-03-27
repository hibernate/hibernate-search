/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.identity.impl;

import java.util.Map;

import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingHelper;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexMappingCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexMappingCollectorValueNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;

class IdentityMappingCollectorPropertyNode<P> extends AbstractIdentityMappingCollectorNode
		implements PojoIndexMappingCollectorPropertyNode {

	private final BoundPojoModelPathPropertyNode<?, P> modelPath;
	private final PojoIdentityMappingCollector identityMappingCollector;

	IdentityMappingCollectorPropertyNode(BoundPojoModelPathPropertyNode<?, P> modelPath,
			PojoMappingHelper mappingHelper,
			PojoIdentityMappingCollector identityMappingCollector) {
		super( mappingHelper );

		this.modelPath = modelPath;

		this.identityMappingCollector = identityMappingCollector;
	}

	@Override
	BoundPojoModelPath getModelPath() {
		return modelPath;
	}

	@Override
	public void propertyBinder(PropertyBinder binder, Map<String, Object> params) {
		// No-op, we're just collecting the identity mapping.
	}

	@Override
	public void identifierBinder(IdentifierBinder binder, Map<String, Object> params) {
		identityMappingCollector.identifierBridge( modelPath, binder, params );
	}

	@Override
	public PojoIndexMappingCollectorValueNode value(ContainerExtractorPath extractorPath) {
		BoundContainerExtractorPath<P, ?> boundExtractorPath =
				mappingHelper.indexModelBinder().bindExtractorPath(
						modelPath.getPropertyModel().typeModel(),
						extractorPath
				);
		return new IdentityMappingCollectorValueNode( modelPath.value( boundExtractorPath ), mappingHelper );
	}
}
