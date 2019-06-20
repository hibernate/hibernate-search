/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorTypeNode;

public class HibernateOrmJpaColumnScaleContributor implements PojoTypeMetadataContributor {

	private final String propertyName;
	private final ContainerExtractorPath extractorPath;
	private final int scale;

	public HibernateOrmJpaColumnScaleContributor(String propertyName, ContainerExtractorPath extractorPath, int scale) {
		this.propertyName = propertyName;
		this.extractorPath = extractorPath;
		this.scale = scale;
	}

	@Override
	public void contributeAdditionalMetadata(PojoAdditionalMetadataCollectorTypeNode collector) {
		collector.property( propertyName ).value( extractorPath ).decimalScale( scale );
	}

	@Override
	public void contributeMapping(PojoMappingCollectorTypeNode collector) {
		// Nothing to do
	}
}
