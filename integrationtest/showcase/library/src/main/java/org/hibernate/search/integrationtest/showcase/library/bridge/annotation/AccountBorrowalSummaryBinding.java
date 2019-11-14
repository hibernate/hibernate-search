/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.bridge.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.integrationtest.showcase.library.bridge.AccountBorrowalSummaryBridge;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@Documented
@TypeMapping(processor = @TypeMappingAnnotationProcessorRef(type = AccountBorrowalSummaryBinding.Processor.class))
public @interface AccountBorrowalSummaryBinding {

	class Processor implements TypeMappingAnnotationProcessor<AccountBorrowalSummaryBinding> {
		@Override
		public void process(TypeMappingStep mapping, AccountBorrowalSummaryBinding annotation,
				TypeMappingAnnotationProcessorContext context) {
			mapping.binder( new AccountBorrowalSummaryBridge.Binder() );
		}
	}
}
