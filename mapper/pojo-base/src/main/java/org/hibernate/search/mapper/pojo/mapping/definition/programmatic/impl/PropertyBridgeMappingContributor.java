/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.Map;

import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexMappingCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoPropertyMetadataContributor;

class PropertyBridgeMappingContributor implements PojoPropertyMetadataContributor {

	private final PropertyBinder binder;
	private final Map<String, Object> params;

	PropertyBridgeMappingContributor(PropertyBinder binder, Map<String, Object> params) {
		this.binder = binder;
		this.params = params;
	}

	@Override
	public void contributeIndexMapping(PojoIndexMappingCollectorPropertyNode collector) {
		collector.propertyBinder(
				binder,
				/*
				 * Ignore mapped types, we don't need to discover new mappings automatically
				 * like in the annotation mappings.
				 */
				params
		);
	}

}
