/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class AssociationInverseSideProcessor implements PropertyMappingAnnotationProcessor<AssociationInverseSide> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public void process(PropertyMappingStep mappingContext, AssociationInverseSide annotation,
			PropertyMappingAnnotationProcessorContext context) {
		ContainerExtractorPath extractorPath = context.toContainerExtractorPath( annotation.extraction() );

		Optional<PojoModelPathValueNode> inversePathOptional =
				context.toPojoModelPathValueNode( annotation.inversePath() );
		if ( !inversePathOptional.isPresent() ) {
			throw log.missingInversePathInAssociationInverseSideMapping();
		}

		mappingContext.associationInverseSide( inversePathOptional.get() )
				.extractors( extractorPath );
	}
}
