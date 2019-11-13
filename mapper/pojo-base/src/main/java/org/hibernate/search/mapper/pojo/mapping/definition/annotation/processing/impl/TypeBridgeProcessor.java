/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.lang.annotation.Annotation;
import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.TypeBinding;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

class TypeBridgeProcessor extends TypeAnnotationProcessor<Annotation> {
	TypeBridgeProcessor(AnnotationProcessorHelper helper) {
		super( helper );
	}

	@Override
	Stream<? extends Annotation> extractAnnotations(PojoRawTypeModel<?> typeModel) {
		return typeModel.getAnnotationsByMetaAnnotationType( TypeBinding.class );
	}

	@Override
	void doProcess(TypeMappingStep mappingContext, PojoRawTypeModel<?> typeModel, Annotation annotation) {
		TypeBinder<?> binder = helper.createTypeBinder( annotation );
		mappingContext.binder( binder );
	}
}
