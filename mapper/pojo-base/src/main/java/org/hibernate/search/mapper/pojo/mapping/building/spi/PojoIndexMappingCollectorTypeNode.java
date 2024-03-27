/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.building.spi;

import java.util.Map;

import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;

public interface PojoIndexMappingCollectorTypeNode extends PojoMappingCollector {

	void typeBinder(TypeBinder builder, Map<String, Object> params);

	PojoIndexMappingCollectorPropertyNode property(String propertyName);

}
