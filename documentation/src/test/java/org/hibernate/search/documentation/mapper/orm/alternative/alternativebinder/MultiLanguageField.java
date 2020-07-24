/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.alternative.alternativebinder;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.bridge.builtin.programmatic.AlternativeBinder;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

//tag::include[]
@Retention(RetentionPolicy.RUNTIME) // <1>
@Target({ ElementType.METHOD, ElementType.FIELD }) // <2>
@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef( // <3>
		type = MultiLanguageField.Processor.class
))
@Documented // <4>
public @interface MultiLanguageField {

	String name() default ""; // <5>

	class Processor implements PropertyMappingAnnotationProcessor<MultiLanguageField> { // <6>
		@Override
		public void process(PropertyMappingStep mapping, MultiLanguageField annotation,
				PropertyMappingAnnotationProcessorContext context) {
			LanguageAlternativeBinderDelegate delegate = new LanguageAlternativeBinderDelegate( // <7>
					annotation.name().isEmpty() ? null : annotation.name()
			);
			mapping.hostingType() // <8>
					.binder( AlternativeBinder.create( // <9>
							Language.class, // <10>
							context.annotatedElement().name(), // <11>
							String.class, // <12>
							BeanReference.ofInstance( delegate ) // <13>
					) );
		}
	}
}
//end::include[]
