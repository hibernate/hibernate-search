/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import java.util.Map;

import org.hibernate.search.engine.mapper.mapping.building.spi.MappingPartialBuildState;

public class StubMappingPartialBuildState implements MappingPartialBuildState {

	private final StubMappingBackendFeatures backendFeatures;
	private final Map<String, StubMappedIndex> mappedIndexesByTypeIdentifier;

	StubMappingPartialBuildState(StubMappingBackendFeatures backendFeatures,
			Map<String, StubMappedIndex> mappedIndexesByTypeIdentifier) {
		this.backendFeatures = backendFeatures;
		this.mappedIndexesByTypeIdentifier = mappedIndexesByTypeIdentifier;
	}

	@Override
	public void closeOnFailure() {
		// Nothing to do
	}

	public StubMappingImpl finalizeMapping(StubMappingSchemaManagementStrategy schemaManagementStrategy) {
		StubMappingImpl mapping = new StubMappingImpl( backendFeatures, mappedIndexesByTypeIdentifier,
				schemaManagementStrategy );
		for ( StubMappedIndex index : mappedIndexesByTypeIdentifier.values() ) {
			index.onMappingCreated( mapping );
		}
		return mapping;
	}

}
