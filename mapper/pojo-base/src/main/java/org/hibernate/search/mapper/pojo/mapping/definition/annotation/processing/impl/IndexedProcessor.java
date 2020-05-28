/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;

public class IndexedProcessor implements TypeMappingAnnotationProcessor<Indexed> {

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
		mappingContext.indexed().backend( backendName ).index( indexName )
				.enabled( annotation.enabled() );
	}
}
