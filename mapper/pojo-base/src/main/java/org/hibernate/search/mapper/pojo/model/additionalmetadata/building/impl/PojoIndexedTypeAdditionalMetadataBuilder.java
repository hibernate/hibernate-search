/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl;

import java.util.Map;
import java.util.Optional;

import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingBinder;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorIndexedTypeNode;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoIndexedTypeAdditionalMetadata;

class PojoIndexedTypeAdditionalMetadataBuilder implements PojoAdditionalMetadataCollectorIndexedTypeNode {

	private Optional<String> backendName = Optional.empty();
	private Optional<String> indexName = Optional.empty();
	private boolean enabled = true;
	private Optional<RoutingBinder> routingBinder = Optional.empty();
	private Map<String, Object> params;

	@Override
	public void backendName(String backendName) {
		this.backendName = Optional.ofNullable( backendName );
	}

	@Override
	public void indexName(String indexName) {
		this.indexName = Optional.ofNullable( indexName );
	}

	@Override
	public void enabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public void routingBinder(RoutingBinder binder, Map<String, Object> params) {
		this.routingBinder = Optional.ofNullable( binder );
		this.params = params;
	}

	public Optional<PojoIndexedTypeAdditionalMetadata> build() {
		if ( enabled ) {
			return Optional.of( new PojoIndexedTypeAdditionalMetadata( backendName, indexName, routingBinder, params ) );
		}
		else {
			return Optional.empty();
		}
	}
}
