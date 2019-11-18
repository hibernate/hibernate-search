/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.IndexingDependencyOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class IndexingDependencyProcessor implements PropertyMappingAnnotationProcessor<IndexingDependency> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

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
					throw log.missingPathInIndexingDependencyDerivedFrom();
				}
				indexingDependencyContext.derivedFrom( pojoModelPathOptional.get() );
			}
		}
	}
}
