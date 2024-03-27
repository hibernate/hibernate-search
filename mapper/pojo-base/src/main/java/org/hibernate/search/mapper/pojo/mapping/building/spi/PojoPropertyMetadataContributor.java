/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.building.spi;

import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorPropertyNode;

public interface PojoPropertyMetadataContributor {

	default void contributeAdditionalMetadata(PojoAdditionalMetadataCollectorPropertyNode collector) {
		// No-op by default
	}

	default void contributeIndexMapping(PojoIndexMappingCollectorPropertyNode collector) {
		// No-op by default
	}

}
