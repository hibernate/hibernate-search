/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.typebridge.simple;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;

//tag::include[]
@Retention(RetentionPolicy.RUNTIME) // <1>
@Target({ ElementType.TYPE }) // <2>
@TypeMapping(processor = @TypeMappingAnnotationProcessorRef( // <3>
		type = FullNameBinding.Processor.class
))
@Documented // <4>
public @interface FullNameBinding {

	class Processor implements TypeMappingAnnotationProcessor<FullNameBinding> { // <5>
		@Override
		public void process(TypeMappingStep mapping, FullNameBinding annotation,
				TypeMappingAnnotationProcessorContext context) {
			mapping.binder( new FullNameBinder() ); // <6>
		}
	}
}
//end::include[]
