/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.valuebridge.parameter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

//tag::include[]
@Retention(RetentionPolicy.RUNTIME) // <1>
@Target({ ElementType.METHOD, ElementType.FIELD }) // <2>
@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef( // <3>
		type = BooleanAsStringField.Processor.class
))
@Documented // <4>
@Repeatable(BooleanAsStringField.List.class) // <5>
public @interface BooleanAsStringField {

	String trueAsString() default "true"; // <6>

	String falseAsString() default "false";

	String name() default ""; // <7>

	ContainerExtraction extraction() default @ContainerExtraction(); // <7>

	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD })
	@Retention(RetentionPolicy.RUNTIME)
	@interface List {
		BooleanAsStringField[] value();
	}

	class Processor implements PropertyMappingAnnotationProcessor<BooleanAsStringField> { // <8>
		@Override
		public void process(PropertyMappingStep mapping, BooleanAsStringField annotation,
				PropertyMappingAnnotationProcessorContext context) {
			BooleanAsStringBridge bridge = new BooleanAsStringBridge( // <9>
					annotation.trueAsString(), annotation.falseAsString()
			);
			mapping.genericField( annotation.name().isEmpty() ? null : annotation.name() ) // <10>
					.valueBridge( bridge ) // <11>
					.extractors( context.toContainerExtractorPath( annotation.extraction() ) ); // <12>
		}
	}
}
//end::include[]
