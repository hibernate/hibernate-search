package org.hibernate.search.mapper.pojo.mapping.building.spi;

import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorTypeNode;

public interface PojoTypeMetadataContributor extends PojoSearchMappingTypeNode {

	default void contributeAdditionalMetadata(PojoAdditionalMetadataCollectorTypeNode collector) {
		// No-op by default
	}

	default void contributeIndexMapping(PojoIndexMappingCollectorTypeNode collector) {
		// No-op by default
	}

}
