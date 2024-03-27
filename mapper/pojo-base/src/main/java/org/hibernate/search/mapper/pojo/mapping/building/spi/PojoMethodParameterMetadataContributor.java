/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.building.spi;

public interface PojoMethodParameterMetadataContributor {

	void contributeSearchMapping(PojoSearchMappingMethodParameterNode collector);

}
