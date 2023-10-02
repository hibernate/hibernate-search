/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.engine.mapper.mapping.building.spi.MappingBuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingConfigurationCollector;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingConfigurationContributor;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

public class ProgrammaticMappingConfigurationContextImpl
		implements ProgrammaticMappingConfigurationContext, PojoMappingConfigurationContributor {

	private final PojoBootstrapIntrospector introspector;

	// Use a LinkedHashMap for deterministic iteration
	private final Map<PojoRawTypeModel<?>, TypeMappingStepImpl> typeMappingContributors = new LinkedHashMap<>();

	public ProgrammaticMappingConfigurationContextImpl(PojoBootstrapIntrospector introspector) {
		this.introspector = introspector;
	}

	@Override
	public void configure(MappingBuildContext buildContext, PojoMappingConfigurationContext configurationContext,
			MappingConfigurationCollector<PojoTypeMetadataContributor> configurationCollector) {
		for ( TypeMappingStepImpl typeMappingContributor : typeMappingContributors.values() ) {
			typeMappingContributor.configure( buildContext, configurationContext, configurationCollector );
		}
	}

	@Override
	public TypeMappingStep type(Class<?> clazz) {
		return type( introspector.typeModel( clazz ) );
	}

	@Override
	public TypeMappingStep type(String typeName) {
		return type( introspector.typeModel( typeName ) );
	}

	private TypeMappingStep type(PojoRawTypeModel<?> typeModel) {
		return typeMappingContributors.computeIfAbsent( typeModel, TypeMappingStepImpl::new );
	}

}
