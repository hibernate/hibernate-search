/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.building.spi;

import java.util.Map;

import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;

public interface PojoIndexMappingCollectorPropertyNode extends PojoMappingCollector {

	void propertyBinder(PropertyBinder binder, Map<String, Object> params);

	void identifierBinder(IdentifierBinder binder, Map<String, Object> params);

	PojoIndexMappingCollectorValueNode value(ContainerExtractorPath extractorPath);

}
