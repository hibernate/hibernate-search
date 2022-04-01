/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import java.util.Map;

import org.hibernate.search.engine.mapper.mapping.building.spi.MappingPartialBuildState;
import org.hibernate.search.engine.search.projection.definition.spi.ProjectionRegistry;

public class StubMappingPartialBuildState implements MappingPartialBuildState {

	private final Map<String, StubMappedIndex> mappedIndexesByTypeIdentifier;
	private final ProjectionRegistry projectionRegistry;

	StubMappingPartialBuildState(Map<String, StubMappedIndex> mappedIndexesByTypeIdentifier,
			ProjectionRegistry projectionRegistry) {
		this.mappedIndexesByTypeIdentifier = mappedIndexesByTypeIdentifier;
		this.projectionRegistry = projectionRegistry;
	}

	@Override
	public void closeOnFailure() {
		// Nothing to do
	}

	public StubMappingImpl finalizeMapping(StubMappingSchemaManagementStrategy schemaManagementStrategy) {
		StubMappingImpl mapping = new StubMappingImpl( mappedIndexesByTypeIdentifier, projectionRegistry,
				schemaManagementStrategy );
		for ( StubMappedIndex index : mappedIndexesByTypeIdentifier.values() ) {
			index.onMappingCreated( mapping );
		}
		return mapping;
	}

}
