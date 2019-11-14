/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.lang.annotation.Annotation;
import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.util.common.reflect.spi.AnnotationHelper;

class IndexedProcessor extends TypeAnnotationProcessor<Indexed> {

	@Override
	public Stream<? extends Indexed> extractAnnotations(Stream<Annotation> annotations,
			AnnotationHelper annotationHelper) {
		return annotations
				.filter( annotation -> Indexed.class.isAssignableFrom( annotation.annotationType() ) )
				.map( Indexed.class::cast );
	}

	@Override
	public void process(TypeMappingStep mappingContext, Indexed annotation,
			TypeMappingAnnotationProcessorContext context) {
		String indexName = annotation.index();
		if ( indexName.isEmpty() ) {
			indexName = null;
		}
		String backendName = annotation.backend();
		if ( backendName.isEmpty() ) {
			backendName = null;
		}
		mappingContext.indexed( backendName, indexName );
	}
}
