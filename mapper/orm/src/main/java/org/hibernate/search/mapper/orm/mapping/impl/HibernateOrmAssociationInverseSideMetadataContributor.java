/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import org.hibernate.search.mapper.pojo.extractor.spi.ContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.augmented.building.spi.PojoAugmentedModelCollectorTypeNode;

final class HibernateOrmAssociationInverseSideMetadataContributor implements PojoTypeMetadataContributor {
	private final String propertyName;
	private final ContainerValueExtractorPath extractorPath;
	private final String inverseSidePropertyName;
	private final ContainerValueExtractorPath inverseSideExtractorPath;

	HibernateOrmAssociationInverseSideMetadataContributor(String propertyName,
			ContainerValueExtractorPath extractorPath,
			String inverseSidePropertyName, ContainerValueExtractorPath inverseSideExtractorPath) {
		this.propertyName = propertyName;
		this.extractorPath = extractorPath;
		this.inverseSidePropertyName = inverseSidePropertyName;
		this.inverseSideExtractorPath = inverseSideExtractorPath;
	}

	@Override
	public void contributeModel(PojoAugmentedModelCollectorTypeNode collector) {
		collector.property( propertyName ).value( extractorPath )
				.associationInverseSide( inverseSidePropertyName, inverseSideExtractorPath );
	}

	@Override
	public void contributeMapping(PojoMappingCollectorTypeNode collector) {
		// Nothing to do
	}
}
