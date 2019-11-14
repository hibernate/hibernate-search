/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.document.model.dsl.object;

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

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef(type = InvoiceLineItemsSummaryBinding.Processor.class))
@Documented
public @interface InvoiceLineItemsSummaryBinding {

	class Processor implements PropertyMappingAnnotationProcessor<InvoiceLineItemsSummaryBinding> {
		@Override
		public void process(PropertyMappingStep mapping, InvoiceLineItemsSummaryBinding annotation,
				PropertyMappingAnnotationProcessorContext context) {
			mapping.binder( new InvoiceLineItemsSummaryBinder() );
		}
	}

}
