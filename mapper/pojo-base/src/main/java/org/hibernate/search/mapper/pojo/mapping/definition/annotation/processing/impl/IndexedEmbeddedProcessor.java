/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

public class IndexedEmbeddedProcessor implements PropertyMappingAnnotationProcessor<IndexedEmbedded> {

	@Override
	@SuppressWarnings("deprecation") // For IndexedEmbedded.prefix
	public void process(PropertyMappingStep mappingContext, IndexedEmbedded annotation,
			PropertyMappingAnnotationProcessorContext context) {
		String cleanedUpPrefix = context.toNullIfDefault( annotation.prefix(), "" );

		String cleanedUpName = context.toNullIfDefault( annotation.name(), "" );

		String[] includePathsArray = annotation.includePaths();
		String[] excludePathsArray = annotation.excludePaths();

		ContainerExtractorPath extractorPath = context.toContainerExtractorPath( annotation.extraction() );

		Class<?> cleanedUpTargetType = context.toNullIfDefault( annotation.targetType(), void.class );

		ObjectStructure structure = annotation.structure();

		mappingContext.indexedEmbedded( cleanedUpName )
				.extractors( extractorPath )
				.prefix( cleanedUpPrefix )
				.structure( structure )
				.includeDepth( context.toNullIfDefault( annotation.includeDepth(), -1 ) )
				.includePaths( MappingAnnotationProcessorUtils.cleanUpPaths( includePathsArray ) )
				.excludePaths( MappingAnnotationProcessorUtils.cleanUpPaths( excludePathsArray ) )
				.includeEmbeddedObjectId( annotation.includeEmbeddedObjectId() )
				.targetType( cleanedUpTargetType );
	}

}
