/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.typebridge.parameter;

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
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@TypeMapping(processor = @TypeMappingAnnotationProcessorRef(type = FullNameBinding.Processor.class))
@Documented
public @interface FullNameBinding {

	boolean sortField() default false; // <1>

	class Processor implements TypeMappingAnnotationProcessor<FullNameBinding> {
		@Override
		public void process(TypeMappingStep mapping, FullNameBinding annotation,
				TypeMappingAnnotationProcessorContext context) {
			FullNameBinder binder = new FullNameBinder() // <2>
					.sortField( annotation.sortField() ); // <3>
			mapping.binder( binder ); // <4>
		}
	}
}
//end::include[]
