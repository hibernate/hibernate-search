/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.Map;

import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingBinder;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingIndexedStep;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorIndexedTypeNode;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

class TypeMappingIndexedStepImpl implements TypeMappingIndexedStep, PojoTypeMetadataContributor {

	private final PojoRawTypeIdentifier<?> typeIdentifier;

	private String backendName;
	private String indexName;
	private Boolean enabled;
	private RoutingBinder binder;
	private Map<String, Object> params;

	TypeMappingIndexedStepImpl(PojoRawTypeIdentifier<?> typeIdentifier) {
		this.typeIdentifier = typeIdentifier;
	}

	@Override
	public TypeMappingIndexedStep backend(String backendName) {
		this.backendName = backendName;
		return this;
	}

	@Override
	public TypeMappingIndexedStep index(String indexName) {
		this.indexName = indexName;
		return this;
	}

	@Override
	public TypeMappingIndexedStep enabled(boolean enabled) {
		this.enabled = enabled;
		return this;
	}

	@Override
	public TypeMappingIndexedStep routingBinder(RoutingBinder binder, Map<String, Object> params) {
		this.binder = binder;
		this.params = params;
		return this;
	}

	@Override
	public void contributeAdditionalMetadata(PojoAdditionalMetadataCollectorTypeNode collector) {
		PojoAdditionalMetadataCollectorIndexedTypeNode indexedCollector = collector.markAsIndexed();
		if ( enabled != null ) {
			indexedCollector.enabled( enabled );
		}
		if ( backendName != null ) {
			indexedCollector.backendName( backendName );
		}
		if ( binder != null ) {
			indexedCollector.routingBinder( binder, params );
		}
		// The fact that an entity is indexed is inherited, but not the index name.
		if ( typeIdentifier.equals( collector.typeIdentifier() ) && indexName != null ) {
			indexedCollector.indexName( indexName );
		}
	}

}
