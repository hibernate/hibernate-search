/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi;

import java.util.Map;

import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBinder;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;

public interface PojoAdditionalMetadataCollectorPropertyNode extends PojoAdditionalMetadataCollector {

	PojoAdditionalMetadataCollectorValueNode value(ContainerExtractorPath extractorPath);

	void markerBinder(MarkerBinder definition, Map<String, Object> params);

}
