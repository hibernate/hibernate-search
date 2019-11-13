/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

class IndexedProcessor extends TypeAnnotationProcessor<Indexed> {

	@Override
	public Stream<? extends Indexed> extractAnnotations(PojoRawTypeModel<?> typeModel) {
		return typeModel.getAnnotationsByType( Indexed.class );
	}

	@Override
	public void process(TypeMappingStep mappingContext, Indexed annotation,
			AnnotationProcessorHelper helper) {
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
