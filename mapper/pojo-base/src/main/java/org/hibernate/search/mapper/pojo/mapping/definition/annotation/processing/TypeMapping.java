/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;

/**
 * Meta-annotation for annotations that apply mapping to a type.
 * <p>
 * Whenever an annotation meta-annotated with {@link TypeMapping}
 * is found on a type in the domain model,
 * the corresponding annotation processor will be retrieved and applied to that type.
 * The processor will be passed the annotation instance through its
 * {@link TypeMappingAnnotationProcessor#process(TypeMappingStep, Annotation, TypeMappingAnnotationProcessorContext)} method.
 */
@Documented
@Target(value = ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TypeMapping {

	/**
	 * @return A reference to the processor to use for the target annotation.
	 * @see TypeMappingAnnotationProcessorRef
	 */
	TypeMappingAnnotationProcessorRef processor();

}
