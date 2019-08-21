/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.mapper;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.engine.mapper.mapping.building.spi.IndexManagerBuildingState;
import org.hibernate.search.engine.mapper.mapping.building.spi.Mapper;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingAbortedException;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.engine.mapper.mapping.spi.MappingBuildContext;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.reporting.spi.EventContexts;

class StubMapper implements Mapper<StubMappingPartialBuildState> {

	private final ContextualFailureCollector failureCollector;
	private final TypeMetadataContributorProvider<StubTypeMetadataContributor> contributorProvider;

	private final Map<StubTypeModel, IndexManagerBuildingState<?>> indexManagerBuildingStates = new HashMap<>();

	StubMapper(MappingBuildContext buildContext,
			TypeMetadataContributorProvider<StubTypeMetadataContributor> contributorProvider) {
		this.failureCollector = buildContext.getFailureCollector();
		this.contributorProvider = contributorProvider;
	}

	@Override
	public void closeOnFailure() {
		// Nothing to do
	}

	@Override
	public void addIndexed(MappableTypeModel typeModel, IndexManagerBuildingState<?> indexManagerBuildingState) {
		indexManagerBuildingStates.put( (StubTypeModel) typeModel, indexManagerBuildingState );
		contributorProvider.get( typeModel ).forEach( c -> c.contribute( indexManagerBuildingState ) );
	}

	@Override
	public StubMappingPartialBuildState prepareBuild() throws MappingAbortedException {
		Map<String, StubMappingIndexManager> indexMappingsByTypeIdentifier = new HashMap<>();
		for ( Map.Entry<StubTypeModel, IndexManagerBuildingState<?>> entry : indexManagerBuildingStates.entrySet() ) {
			StubTypeModel typeModel = entry.getKey();
			try {
				MappedIndexManager<?> indexManager = entry.getValue().build();
				indexMappingsByTypeIdentifier.put(
						typeModel.asString(),
						new StubMappingIndexManager( indexManager )
				);
			}
			catch (RuntimeException e) {
				failureCollector.withContext( EventContexts.fromType( typeModel ) ).add( e );
			}
		}

		if ( failureCollector.hasFailure() ) {
			throw new MappingAbortedException();
		}

		return new StubMappingPartialBuildState( indexMappingsByTypeIdentifier );
	}
}
