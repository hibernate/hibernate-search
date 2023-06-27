/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.annotations.impl;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;

@Deprecated
public class IndexedAnnotationProcessor implements TypeMappingAnnotationProcessor<Indexed> {
	@Override
	public void process(TypeMappingStep mapping, Indexed annotation, TypeMappingAnnotationProcessorContext context) {
		String indexName = context.toNullIfDefault( annotation.index(), "" );
		mapping.indexed().index( indexName );
	}
}
