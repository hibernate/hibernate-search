/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoPropertyMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.AssociationInverseSideOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;



class AssociationInverseSideOptionsStepImpl
		extends DelegatingPropertyMappingStep
		implements AssociationInverseSideOptionsStep, PojoPropertyMetadataContributor {

	private final PojoModelPathValueNode inversePath;
	private ContainerExtractorPath extractorPath = ContainerExtractorPath.defaultExtractors();

	AssociationInverseSideOptionsStepImpl(PropertyMappingStep delegate, PojoModelPathValueNode inversePath) {
		super( delegate );
		this.inversePath = inversePath;
	}

	@Override
	public void contributeAdditionalMetadata(PojoAdditionalMetadataCollectorPropertyNode collector) {
		collector.value( extractorPath ).associationInverseSide( inversePath );
	}

	@Override
	public AssociationInverseSideOptionsStep extractors(ContainerExtractorPath extractorPath) {
		this.extractorPath = extractorPath;
		return this;
	}
}
