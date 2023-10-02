/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.Map;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.BeanBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexMappingCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoPropertyMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingDocumentIdOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

class PropertyMappingDocumentIdOptionsStepImpl extends DelegatingPropertyMappingStep
		implements PropertyMappingDocumentIdOptionsStep, PojoPropertyMetadataContributor {

	private IdentifierBinder binder;
	private Map<String, Object> params;

	PropertyMappingDocumentIdOptionsStepImpl(PropertyMappingStep parent) {
		super( parent );
	}

	@Override
	public void contributeIndexMapping(PojoIndexMappingCollectorPropertyNode collector) {
		collector.identifierBinder( binder, params );
	}

	@Override
	public PropertyMappingDocumentIdOptionsStep identifierBridge(Class<? extends IdentifierBridge<?>> bridgeClass) {
		return identifierBridge( BeanReference.of( bridgeClass ) );
	}

	@Override
	public PropertyMappingDocumentIdOptionsStep identifierBridge(BeanReference<? extends IdentifierBridge<?>> bridgeReference) {
		return identifierBinder( new BeanBinder( bridgeReference ) );
	}

	@Override
	public PropertyMappingDocumentIdOptionsStep identifierBinder(IdentifierBinder binder, Map<String, Object> params) {
		this.binder = binder;
		this.params = params;
		return this;
	}
}
