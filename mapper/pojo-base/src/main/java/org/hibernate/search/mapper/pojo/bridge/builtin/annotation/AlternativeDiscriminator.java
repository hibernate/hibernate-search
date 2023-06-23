/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.processor.impl.AlternativeDiscriminatorProcessor;
import org.hibernate.search.mapper.pojo.bridge.builtin.programmatic.AlternativeBinder;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorRef;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Mark the property as an alternative discriminator for use in {@link AlternativeBinder}.
 *
 * @see AlternativeBinder
 */
@Incubating
@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef(type = AlternativeDiscriminatorProcessor.class,
		retrieval = BeanRetrieval.CONSTRUCTOR))
public @interface AlternativeDiscriminator {

	/**
	 * @return identifier of the alternative.
	 * This is used to differentiate between multiple alternative discriminators.
	 * @see AlternativeBinder#alternativeId(String)
	 */
	String id() default "";

}
