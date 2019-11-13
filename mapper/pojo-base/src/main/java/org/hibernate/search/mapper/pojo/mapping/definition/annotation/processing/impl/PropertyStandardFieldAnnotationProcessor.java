/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.lang.annotation.Annotation;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStandardFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;

abstract class PropertyStandardFieldAnnotationProcessor<A extends Annotation>
		extends PropertyFieldAnnotationProcessor<A> {

	PropertyStandardFieldAnnotationProcessor(AnnotationProcessorHelper helper, Class<A> annotationType) {
		super( helper, annotationType );
	}

	@Override
	PropertyMappingFieldOptionsStep<?> initFieldMappingContext(PropertyMappingStep mappingContext,
			PojoPropertyModel<?> propertyModel, A annotation, String fieldName) {
		PropertyMappingStandardFieldOptionsStep<?> fieldContext =
				initStandardFieldMappingContext( mappingContext, propertyModel, annotation, fieldName );

		Projectable projectable = getProjectable( annotation );
		if ( !Projectable.DEFAULT.equals( projectable ) ) {
			fieldContext.projectable( projectable );
		}

		Searchable searchable = getSearchable( annotation );
		if ( !Searchable.DEFAULT.equals( searchable ) ) {
			fieldContext.searchable( searchable );
		}

		return fieldContext;
	}

	abstract PropertyMappingStandardFieldOptionsStep<?> initStandardFieldMappingContext(PropertyMappingStep mappingContext,
			PojoPropertyModel<?> propertyModel, A annotation, String fieldName);

	abstract Projectable getProjectable(A annotation);

	abstract Searchable getSearchable(A annotation);
}
