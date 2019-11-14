/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.smoke.bridge;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@TypeMapping(processor = @TypeMappingAnnotationProcessorRef(type = CustomTypeBinding.Processor.class))
public @interface CustomTypeBinding {

	String objectName();

	class Processor implements TypeMappingAnnotationProcessor<CustomTypeBinding> {
		@Override
		public void process(TypeMappingStep mapping, CustomTypeBinding annotation,
				TypeMappingAnnotationProcessorContext context) {
			mapping.binder( new CustomTypeBridge.Binder().objectName( annotation.objectName() ) );
		}
	}
}
