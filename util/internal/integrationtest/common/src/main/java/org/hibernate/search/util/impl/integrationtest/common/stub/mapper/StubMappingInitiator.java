/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.engine.mapper.mapping.building.spi.Mapper;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingConfigurationCollector;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingInitiator;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.engine.mapper.mapping.spi.MappingBuildContext;

public class StubMappingInitiator implements MappingInitiator<StubTypeMetadataContributor, StubMappingPartialBuildState> {

	private final boolean multiTenancyEnabled;
	private final List<StubTypeMetadataContributor> mappingContributors = new ArrayList<>();

	public StubMappingInitiator(boolean multiTenancyEnabled) {
		this.multiTenancyEnabled = multiTenancyEnabled;
	}

	public void add(String typeIdentifier, String backendName, String indexName,
			Consumer<? super IndexedEntityBindingContext> mappingContributor) {
		mappingContributors.add( new StubTypeMetadataContributor( new StubTypeModel( typeIdentifier ), backendName,
				indexName, mappingContributor ) );
	}

	@Override
	public void configure(MappingBuildContext buildContext,
			MappingConfigurationCollector<StubTypeMetadataContributor> configurationCollector) {
		for ( StubTypeMetadataContributor mappingContributor : mappingContributors ) {
			mappingContributor.contribute( configurationCollector );
		}
	}

	@Override
	public Mapper<StubMappingPartialBuildState> createMapper(MappingBuildContext buildContext,
			TypeMetadataContributorProvider<StubTypeMetadataContributor> contributorProvider) {
		return new StubMapper( buildContext, contributorProvider, multiTenancyEnabled );
	}

}
