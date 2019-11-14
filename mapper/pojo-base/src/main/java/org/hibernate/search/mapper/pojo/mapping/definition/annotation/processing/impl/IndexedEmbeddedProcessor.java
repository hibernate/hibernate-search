/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;

class IndexedEmbeddedProcessor extends PropertyAnnotationProcessor<IndexedEmbedded> {

	@Override
	public Stream<? extends IndexedEmbedded> extractAnnotations(PojoPropertyModel<?> propertyModel) {
		return propertyModel.getAnnotationsByType( IndexedEmbedded.class );
	}

	@Override
	public void process(PropertyMappingStep mappingContext, IndexedEmbedded annotation,
			PropertyMappingAnnotationProcessorContext context) {
		String cleanedUpPrefix = annotation.prefix();
		if ( cleanedUpPrefix.isEmpty() ) {
			cleanedUpPrefix = null;
		}

		Integer cleanedUpMaxDepth = annotation.maxDepth();
		if ( cleanedUpMaxDepth.equals( -1 ) ) {
			cleanedUpMaxDepth = null;
		}

		String[] includePathsArray = annotation.includePaths();
		Set<String> cleanedUpIncludePaths;
		if ( includePathsArray.length > 0 ) {
			cleanedUpIncludePaths = new HashSet<>();
			Collections.addAll( cleanedUpIncludePaths, includePathsArray );
		}
		else {
			cleanedUpIncludePaths = Collections.emptySet();
		}

		ContainerExtractorPath extractorPath = context.toContainerExtractorPath( annotation.extraction() );

		mappingContext.indexedEmbedded()
				.extractors( extractorPath )
				.prefix( cleanedUpPrefix )
				.storage( annotation.storage() )
				.maxDepth( cleanedUpMaxDepth )
				.includePaths( cleanedUpIncludePaths );
	}
}
