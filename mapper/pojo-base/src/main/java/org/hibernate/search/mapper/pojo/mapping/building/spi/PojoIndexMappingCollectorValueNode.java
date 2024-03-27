/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.building.spi;

import java.util.Map;

import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.common.tree.TreeFilterDefinition;
import org.hibernate.search.mapper.pojo.bridge.binding.spi.FieldModelContributor;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public interface PojoIndexMappingCollectorValueNode extends PojoMappingCollector {

	void valueBinder(ValueBinder binder, Map<String, Object> params,
			String relativeFieldName, FieldModelContributor fieldModelContributor);

	void indexedEmbedded(PojoRawTypeIdentifier<?> definingType, String relativePrefix, ObjectStructure structure,
			TreeFilterDefinition filterDefinition, boolean includeEmbeddedObjectId, Class<?> targetType);

}
