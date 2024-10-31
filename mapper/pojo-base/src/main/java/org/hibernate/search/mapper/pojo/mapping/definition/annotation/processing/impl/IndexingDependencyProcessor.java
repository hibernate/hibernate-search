/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.util.Optional;

import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.logging.impl.MappingLog;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.IndexingDependencyOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;

public class IndexingDependencyProcessor implements PropertyMappingAnnotationProcessor<IndexingDependency> {

	@Override
	public void process(PropertyMappingStep mappingContext, IndexingDependency annotation,
			PropertyMappingAnnotationProcessorContext context) {
		ContainerExtractorPath extractorPath = context.toContainerExtractorPath( annotation.extraction() );

		ReindexOnUpdate reindexOnUpdate = annotation.reindexOnUpdate();

		IndexingDependencyOptionsStep indexingDependencyContext = mappingContext.indexingDependency()
				.extractors( extractorPath );

		indexingDependencyContext.reindexOnUpdate( reindexOnUpdate );

		ObjectPath[] derivedFromAnnotations = annotation.derivedFrom();
		if ( derivedFromAnnotations.length > 0 ) {
			for ( ObjectPath objectPath : annotation.derivedFrom() ) {
				Optional<PojoModelPathValueNode> pojoModelPathOptional = context.toPojoModelPathValueNode( objectPath );
				if ( !pojoModelPathOptional.isPresent() ) {
					throw MappingLog.INSTANCE.missingPathInIndexingDependencyDerivedFrom();
				}
				indexingDependencyContext.derivedFrom( pojoModelPathOptional.get() );
			}
		}
	}
}
