/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.impl;

import org.hibernate.search.engine.mapper.mapping.building.spi.MappingBuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingConfigurationCollector;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingConfigurationContributor;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.standalone.mapping.metadata.impl.StandalonePojoEntityTypeMetadataProvider;

class StandalonePojoTypeConfigurationContributor implements PojoMappingConfigurationContributor {

	private final StandalonePojoEntityTypeMetadataProvider entityTypeMetadataProvider;

	public StandalonePojoTypeConfigurationContributor(
			StandalonePojoEntityTypeMetadataProvider entityTypeMetadataProvider) {
		this.entityTypeMetadataProvider = entityTypeMetadataProvider;
	}

	@Override
	public void configure(MappingBuildContext buildContext, PojoMappingConfigurationContext configurationContext,
			MappingConfigurationCollector<PojoTypeMetadataContributor> configurationCollector) {
		for ( PojoRawTypeModel<?> type : entityTypeMetadataProvider.allEntityTypes() ) {
			configurationCollector.collectContributor(
					type,
					new StandalonePojoEntityTypeContributor(
							type.typeIdentifier(), entityTypeMetadataProvider.get( type ).name )
			);
		}
	}
}
