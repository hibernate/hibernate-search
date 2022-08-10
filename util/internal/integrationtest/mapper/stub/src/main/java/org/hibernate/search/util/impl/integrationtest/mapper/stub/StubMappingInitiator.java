/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.mapper.mapping.building.spi.Mapper;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingBuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingConfigurationCollector;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingInitiator;
import org.hibernate.search.engine.mapper.model.spi.TypeMetadataContributorProvider;
import org.hibernate.search.engine.tenancy.spi.TenancyMode;

public class StubMappingInitiator implements MappingInitiator<StubMappedIndex, StubMappingPartialBuildState> {

	private final TenancyMode tenancyMode;
	private final List<StubMappedIndex> mappedIndexes = new ArrayList<>();

	public StubMappingInitiator(TenancyMode tenancyMode) {
		this.tenancyMode = tenancyMode;
	}

	public void add(StubMappedIndex mappedIndex) {
		mappedIndexes.add( mappedIndex );
	}

	@Override
	public void configure(MappingBuildContext buildContext,
			MappingConfigurationCollector<StubMappedIndex> configurationCollector) {
		for ( StubMappedIndex mappedIndex : mappedIndexes ) {
			configurationCollector.collectContributor( new StubTypeModel( mappedIndex.typeName() ), mappedIndex );
		}
	}

	@Override
	public Mapper<StubMappingPartialBuildState> createMapper(MappingBuildContext buildContext,
			TypeMetadataContributorProvider<StubMappedIndex> contributorProvider) {
		return new StubMapper( buildContext, contributorProvider, tenancyMode );
	}

}
