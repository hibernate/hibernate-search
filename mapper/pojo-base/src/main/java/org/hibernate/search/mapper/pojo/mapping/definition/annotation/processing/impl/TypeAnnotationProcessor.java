/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.lang.annotation.Annotation;
import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

public abstract class TypeAnnotationProcessor<A extends Annotation> {

	public abstract Stream<? extends A> extractAnnotations(PojoRawTypeModel<?> typeModel);

	public abstract void process(TypeMappingStep mappingContext, A annotation,
			TypeMappingAnnotationProcessorContext context);
}
