/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.identifierbridge.param.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

//tag::include[]
@Retention(RetentionPolicy.RUNTIME) // <1>
@Target({ ElementType.METHOD, ElementType.FIELD }) // <2>
@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef( // <3>
		type = OffsetDocumentId.Processor.class
))
@Documented // <4>
public @interface OffsetDocumentId {

	int offset(); // <5>

	class Processor // <6>
			implements PropertyMappingAnnotationProcessor<OffsetDocumentId> { // <7>
		@Override
		public void process(PropertyMappingStep mapping, OffsetDocumentId annotation,
				PropertyMappingAnnotationProcessorContext context) {
			OffsetIdentifierBridge bridge = new OffsetIdentifierBridge( // <8>
					annotation.offset()
			);
			mapping.documentId() // <9>
					.identifierBridge( bridge ); // <10>
		}
	}
}
//end::include[]
