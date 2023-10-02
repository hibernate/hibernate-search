/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.mapper.mapping.building.spi.MappingBuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingConfigurationCollector;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.mapping.building.spi.ErrorCollectingPojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoSearchMappingConstructorNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ConstructorMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingIndexedStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingSearchEntityStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingConfigurationContributor;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

public class TypeMappingStepImpl
		implements TypeMappingStep, PojoMappingConfigurationContributor, PojoTypeMetadataContributor {

	private final PojoRawTypeModel<?> typeModel;

	private final ErrorCollectingPojoTypeMetadataContributor children = new ErrorCollectingPojoTypeMetadataContributor();

	private Map<List<Class<?>>, InitialConstructorMappingStep> constructors;

	public TypeMappingStepImpl(PojoRawTypeModel<?> typeModel) {
		this.typeModel = typeModel;
	}

	@Override
	public void configure(MappingBuildContext buildContext, PojoMappingConfigurationContext configurationContext,
			MappingConfigurationCollector<PojoTypeMetadataContributor> configurationCollector) {
		configurationCollector.collectContributor( typeModel, this );
	}

	@Override
	public void contributeAdditionalMetadata(PojoAdditionalMetadataCollectorTypeNode collector) {
		children.contributeAdditionalMetadata( collector );
	}

	@Override
	public void contributeIndexMapping(PojoIndexMappingCollectorTypeNode collector) {
		children.contributeIndexMapping( collector );
	}

	@Override
	public TypeMappingIndexedStep indexed() {
		TypeMappingIndexedStepImpl child = new TypeMappingIndexedStepImpl( typeModel.typeIdentifier() );
		children.add( child );
		return child;
	}

	@Override
	public TypeMappingSearchEntityStep searchEntity() {
		TypeMappingSearchEntityStepImpl child = new TypeMappingSearchEntityStepImpl( typeModel.typeIdentifier() );
		children.add( child );
		return child;
	}

	@Override
	public TypeMappingStep binder(TypeBinder binder, Map<String, Object> params) {
		children.add( new TypeBridgeMappingContributor( binder, params ) );
		return this;
	}

	@Override
	public ConstructorMappingStep mainConstructor() {
		return constructor( typeModel.mainConstructor().parametersJavaTypes() );
	}

	@Override
	public ConstructorMappingStep constructor(Class<?>... parameterTypes) {
		if ( constructors == null ) {
			constructors = new LinkedHashMap<>();
		}
		List<Class<?>> key = Arrays.asList( parameterTypes );
		InitialConstructorMappingStep result = constructors.get( key );
		if ( result == null ) {
			result = new InitialConstructorMappingStep( this, typeModel.constructor( parameterTypes ) );
			constructors.put( key, result );
		}
		return result;
	}

	@Override
	public Map<List<Class<?>>, ? extends PojoSearchMappingConstructorNode> constructors() {
		return constructors == null ? Collections.emptyMap() : constructors;
	}

	@Override
	public PropertyMappingStep property(String propertyName) {
		PojoPropertyModel<?> propertyModel = typeModel.property( propertyName );
		InitialPropertyMappingStep child = new InitialPropertyMappingStep( this, propertyModel );
		children.add( child );
		return child;
	}

	PojoRawTypeModel<?> getTypeModel() {
		return typeModel;
	}
}
