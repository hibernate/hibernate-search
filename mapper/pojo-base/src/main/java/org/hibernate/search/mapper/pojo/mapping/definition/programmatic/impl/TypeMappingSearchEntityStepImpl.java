/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.Map;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.spi.ParameterizedBeanReference;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingSearchEntityStep;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

class TypeMappingSearchEntityStepImpl implements TypeMappingSearchEntityStep, PojoTypeMetadataContributor {

	private final PojoRawTypeIdentifier<?> typeIdentifier;

	private String entityName;
	private ParameterizedBeanReference<?> loadingBinderRef;

	TypeMappingSearchEntityStepImpl(PojoRawTypeIdentifier<?> typeIdentifier) {
		this.typeIdentifier = typeIdentifier;
	}

	@Override
	public TypeMappingSearchEntityStep name(String entityName) {
		this.entityName = entityName;
		return this;
	}

	@Override
	public TypeMappingSearchEntityStep loadingBinder(BeanReference<?> binderRef, Map<String, Object> params) {
		this.loadingBinderRef = ParameterizedBeanReference.of( binderRef, params );
		return this;
	}

	@Override
	public void contributeAdditionalMetadata(PojoAdditionalMetadataCollectorTypeNode collector) {
		if ( !typeIdentifier.equals( collector.typeIdentifier() ) ) {
			// Entity metadata is not inherited; only contribute it to the exact type.
			return;
		}
		var node = collector.markAsEntity();
		if ( entityName != null ) {
			node.entityName( entityName );
		}
		if ( loadingBinderRef != null ) {
			node.loadingBinder( loadingBinderRef );
		}
	}

}
