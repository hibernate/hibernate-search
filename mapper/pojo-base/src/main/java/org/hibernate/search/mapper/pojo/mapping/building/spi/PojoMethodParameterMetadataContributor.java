package org.hibernate.search.mapper.pojo.mapping.building.spi;

public interface PojoMethodParameterMetadataContributor {

	void contributeSearchMapping(PojoSearchMappingMethodParameterNode collector);

}
