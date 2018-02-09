/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.util.common.stub.mapper;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.index.spi.IndexManager;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexManagerBuildingState;
import org.hibernate.search.engine.mapper.mapping.building.spi.Mapper;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.engine.mapper.model.spi.IndexedTypeIdentifier;

class StubMapper implements Mapper<StubMappingContributor, StubMapping> {

	private final Map<StubTypeIdentifier, IndexManagerBuildingState<?>> indexManagerBuildingStates = new HashMap<>();

	@Override
	public void addIndexed(IndexedTypeIdentifier typeId, IndexManagerBuildingState<?> indexManagerBuildingState,
			TypeMetadataContributorProvider<StubMappingContributor> contributorProvider) {
		indexManagerBuildingStates.put( (StubTypeIdentifier) typeId, indexManagerBuildingState );
		contributorProvider.get( typeId ).forEach( c -> c.contribute( indexManagerBuildingState ) );
	}

	@Override
	public StubMapping build() {
		Map<String, String> normalizedIndexNamesByTypeIdentifier = indexManagerBuildingStates.entrySet().stream()
				.collect( Collectors.toMap( e -> e.getKey().asString(), e -> e.getValue().getIndexName() ) );
		Map<String, IndexManager<?>> indexManagersByTypeIdentifier = indexManagerBuildingStates.entrySet().stream()
				.collect( Collectors.toMap( e -> e.getKey().asString(), e -> e.getValue().build() ) );
		return new StubMapping( normalizedIndexNamesByTypeIdentifier, indexManagersByTypeIdentifier );
	}
}
