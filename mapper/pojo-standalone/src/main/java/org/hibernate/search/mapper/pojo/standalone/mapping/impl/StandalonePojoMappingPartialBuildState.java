/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.mapping.impl;

import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingFinalizationContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingPartialBuildState;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.standalone.cfg.StandalonePojoMapperSettings;
import org.hibernate.search.mapper.pojo.standalone.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.mapper.pojo.standalone.schema.management.impl.SchemaManagementListener;

public class StandalonePojoMappingPartialBuildState implements MappingPartialBuildState {

	private static final ConfigurationProperty<SchemaManagementStrategyName> SCHEMA_MANAGEMENT_STRATEGY =
			ConfigurationProperty.forKey( StandalonePojoMapperSettings.Radicals.SCHEMA_MANAGEMENT_STRATEGY )
					.as( SchemaManagementStrategyName.class, SchemaManagementStrategyName::of )
					.withDefault( StandalonePojoMapperSettings.Defaults.SCHEMA_MANAGEMENT_STRATEGY )
					.build();

	private final PojoMappingDelegate mappingDelegate;
	private final StandalonePojoTypeContextContainer typeContextContainer;

	StandalonePojoMappingPartialBuildState(PojoMappingDelegate mappingDelegate,
			StandalonePojoTypeContextContainer typeContextContainer) {
		this.mappingDelegate = mappingDelegate;
		this.typeContextContainer = typeContextContainer;
	}

	@Override
	public void closeOnFailure() {
		mappingDelegate.close();
	}

	public StandalonePojoMapping finalizeMapping(MappingFinalizationContext context) {
		SchemaManagementStrategyName schemaManagementStrategyName = SCHEMA_MANAGEMENT_STRATEGY.get(
				context.configurationPropertySource() );
		SchemaManagementListener schemaManagementListener = new SchemaManagementListener( schemaManagementStrategyName );
		return new StandalonePojoMapping( mappingDelegate, typeContextContainer, schemaManagementListener );
	}

}
