/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.mapper;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.Mapper;
import org.hibernate.search.engine.mapper.mapping.building.spi.MapperFactory;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.engine.mapper.mapping.spi.MappingKey;

class StubMapperFactory
		implements MapperFactory<StubTypeMetadataContributor, StubMapping>, MappingKey<StubMapping> {

	private final boolean multiTenancyEnabled;

	public StubMapperFactory(boolean multiTenancyEnabled) {
		this.multiTenancyEnabled = multiTenancyEnabled;
	}

	@Override
	public MappingKey<StubMapping> getMappingKey() {
		return this;
	}

	@Override
	public Mapper<StubMapping> createMapper(BuildContext buildContext, ConfigurationPropertySource propertySource,
			TypeMetadataContributorProvider<StubTypeMetadataContributor> contributorProvider) {
		return new StubMapper( contributorProvider, multiTenancyEnabled );
	}

}
