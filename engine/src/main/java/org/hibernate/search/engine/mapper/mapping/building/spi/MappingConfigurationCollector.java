/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.engine.mapper.model.spi.TypeMetadataDiscoverer;

/**
 * @param <C> The Java type of type metadata contributors
 */
public interface MappingConfigurationCollector<C> {

	void collectContributor(MappableTypeModel typeModel, C contributor);

	void collectDiscoverer(TypeMetadataDiscoverer<C> metadataDiscoverer);

}
