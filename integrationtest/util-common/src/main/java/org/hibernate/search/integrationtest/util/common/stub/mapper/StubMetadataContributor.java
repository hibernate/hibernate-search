/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.util.common.stub.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.engine.common.SearchMappingRepositoryBuilder;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MetadataContributor;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataCollector;

public class StubMetadataContributor implements MetadataContributor {

	private final SearchMappingRepositoryBuilder searchBuilder;
	private final StubMapperFactory mapperFactory;
	private final List<StubMappingContributor> mappingContributors = new ArrayList<>();

	public StubMetadataContributor(SearchMappingRepositoryBuilder searchBuilder) {
		this.searchBuilder = searchBuilder;
		StubTypeOrdering typeOrdering = new StubTypeOrdering();
		this.mapperFactory = new StubMapperFactory( typeOrdering );
		searchBuilder.addMapping( this );
	}

	public void add(String typeIdentifier, String indexName, Consumer<IndexModelBindingContext> mappingContributor) {
		mappingContributors.add( new StubMappingContributor( new StubTypeIdentifier( typeIdentifier ), indexName, mappingContributor ) );
	}

	@Override
	public void contribute(BuildContext buildContext, TypeMetadataCollector collector) {
		for ( StubMappingContributor mappingContributor : mappingContributors ) {
			mappingContributor.contribute( mapperFactory, collector );
		}
	}

	public StubMapping getResult() {
		return searchBuilder.getBuiltResult().getMapping( mapperFactory );
	}
}
