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
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorValueNode;

final class HibernateOrmAssociationEmbeddedMetadataContributor implements PojoTypeMetadataContributor {
	private final String propertyName;
	private final ContainerExtractorPath extractorPath;

	HibernateOrmAssociationEmbeddedMetadataContributor(String propertyName, ContainerExtractorPath extractorPath) {
		this.propertyName = propertyName;
		this.extractorPath = extractorPath;
	}

	@Override
	public void contributeAdditionalMetadata(PojoAdditionalMetadataCollectorTypeNode collector) {
		PojoAdditionalMetadataCollectorValueNode collectorValueNode =
				collector.property( propertyName ).value( extractorPath );
		try {
			collectorValueNode.associationEmbedded();
		}
		catch (RuntimeException e) {
			collectorValueNode.getFailureCollector().add( e );
		}
	}

	@Override
	public void contributeMapping(PojoMappingCollectorTypeNode collector) {
		// Nothing to do
	}
}
