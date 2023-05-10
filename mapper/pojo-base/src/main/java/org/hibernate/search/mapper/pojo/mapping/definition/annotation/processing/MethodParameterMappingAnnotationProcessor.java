/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing;

import java.lang.annotation.Annotation;

import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep;

/**
 * A processor for mapping annotations applied to a Java method parameter.
 * <p>
 * Implementations extract information from the annotation,
 * and according to that information,
 * contribute to the mapping passed in parameter.
 *
 * @param <A> The type of annotations supported by this processor.
 */
public interface MethodParameterMappingAnnotationProcessor<A extends Annotation> {

	/**
	 * Extract information from the annotation and, in accordance with that information,
	 * contribute to the mapping passed in parameter.
	 * @param mapping The mapping to contribute to, targeting the method parameter that the annotation was applied to.
	 * @param annotation The annotation to process.
	 * @param context A context providing various information and helpers.
	 */
	void process(MethodParameterMappingStep mapping, A annotation, MethodParameterMappingAnnotationProcessorContext context);

}
