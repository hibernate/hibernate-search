/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.bootstrap.spi;

import java.util.Map;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;

public interface HibernateSearchOrmMappingProducer {

	Map<Class<?>, JaxbEntityMappingsImpl> produceMappings(
			ConfigurationPropertySource propertySource,
			MetadataBuildingContext buildingContext
	);

}
