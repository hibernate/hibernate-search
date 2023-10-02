/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
