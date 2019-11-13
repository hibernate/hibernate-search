/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

class DocumentIdProcessor extends PropertyAnnotationProcessor<DocumentId> {
	DocumentIdProcessor(AnnotationProcessorHelper helper) {
		super( helper );
	}

	@Override
	Stream<? extends DocumentId> extractAnnotations(PojoPropertyModel<?> propertyModel) {
		return propertyModel.getAnnotationsByType( DocumentId.class );
	}

	@Override
	void doProcess(PropertyMappingStep mappingContext,
			PojoRawTypeModel<?> typeModel, PojoPropertyModel<?> propertyModel,
			DocumentId annotation) {
		IdentifierBinder binder =
				helper.createIdentifierBinder( annotation, propertyModel );

		mappingContext.documentId().identifierBinder( binder );
	}
}
