/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.impl;

import java.lang.annotation.Annotation;

import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationDefaultValues;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingNonFullTextFieldOptionsStep;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;

abstract class PropertyNotFullTextFieldAnnotationProcessor<A extends Annotation> extends PropertyFieldAnnotationProcessor<A> {
	PropertyNotFullTextFieldAnnotationProcessor(AnnotationProcessorHelper helper, Class<A> annotationType) {
		super( helper, annotationType );
	}

	@Override
	final PropertyMappingFieldOptionsStep<?> initFieldMappingContext(PropertyMappingStep mappingContext,
			PojoPropertyModel<?> propertyModel, A annotation, String fieldName) {
		PropertyMappingNonFullTextFieldOptionsStep<?> fieldContext = initSortableFieldMappingContext(
				mappingContext, propertyModel, annotation, fieldName
		);

		Sortable sortable = getSortable( annotation );
		if ( !Sortable.DEFAULT.equals( sortable ) ) {
			fieldContext.sortable( sortable );
		}

		String indexNullAs = getIndexNullAs( annotation );
		if ( indexNullAs != null && !AnnotationDefaultValues.DO_NOT_INDEX_NULL.equals( indexNullAs ) ) {
			fieldContext.indexNullAs( indexNullAs );
		}

		return fieldContext;
	}

	abstract PropertyMappingNonFullTextFieldOptionsStep<?> initSortableFieldMappingContext(PropertyMappingStep mappingContext,
			PojoPropertyModel<?> propertyModel, A annotation, String fieldName);

	abstract Sortable getSortable(A annotation);

	abstract String getIndexNullAs(A annotation);
}
