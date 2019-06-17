/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoPropertyMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.AssociationInverseSideMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingContext;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;



public class AssociationInverseSideMappingContextImpl
		extends DelegatingPropertyMappingContext
		implements AssociationInverseSideMappingContext, PojoPropertyMetadataContributor {

	private final PojoModelPathValueNode inversePath;
	private ContainerExtractorPath extractorPath = ContainerExtractorPath.defaultExtractors();

	AssociationInverseSideMappingContextImpl(PropertyMappingContext delegate, PojoModelPathValueNode inversePath) {
		super( delegate );
		this.inversePath = inversePath;
	}

	@Override
	public void contributeAdditionalMetadata(PojoAdditionalMetadataCollectorPropertyNode collector) {
		collector.value( extractorPath ).associationInverseSide( inversePath );
	}

	@Override
	public void contributeMapping(PojoMappingCollectorPropertyNode collector) {
		// Nothing to do
	}

	@Override
	public AssociationInverseSideMappingContext withExtractors(ContainerExtractorPath extractorPath) {
		this.extractorPath = extractorPath;
		return this;
	}
}
