/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.annotations.impl;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

public class DocumentIdAnnotationProcessor implements PropertyMappingAnnotationProcessor<DocumentId> {
	@Override
	public void process(PropertyMappingStep mapping, DocumentId annotation,
			PropertyMappingAnnotationProcessorContext context) {
		mapping.documentId();
	}
}
