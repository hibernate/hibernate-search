/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.impl;

import java.lang.annotation.Annotation;

import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyFieldMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertySortableFieldMappingContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;

abstract class PropertySortableFieldAnnotationProcessor<A extends Annotation> extends PropertyFieldAnnotationProcessor<A> {
	PropertySortableFieldAnnotationProcessor(AnnotationProcessorHelper helper, Class<A> annotationType) {
		super( helper, annotationType );
	}

	@Override
	final PropertyFieldMappingContext<?> initFieldMappingContext(PropertyMappingContext mappingContext,
			PojoPropertyModel<?> propertyModel, A annotation, String fieldName) {
		PropertySortableFieldMappingContext<?> fieldContext = initSortableFieldMappingContext(
				mappingContext, propertyModel, annotation, fieldName
		);

		Sortable sortable = getSortable( annotation );
		if ( !Sortable.DEFAULT.equals( sortable ) ) {
			fieldContext.sortable( sortable );
		}

		return fieldContext;
	}

	abstract PropertySortableFieldMappingContext<?> initSortableFieldMappingContext(PropertyMappingContext mappingContext,
			PojoPropertyModel<?> propertyModel, A annotation, String fieldName);

	abstract Sortable getSortable(A annotation);
}
