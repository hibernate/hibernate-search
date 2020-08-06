/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.BeanDelegatingBinder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

@SuppressWarnings("deprecation")
public final class RoutingKeyBindingProcessor
		implements TypeMappingAnnotationProcessor<org.hibernate.search.mapper.pojo.mapping.definition.annotation.RoutingKeyBinding> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public void process(TypeMappingStep mapping,
			org.hibernate.search.mapper.pojo.mapping.definition.annotation.RoutingKeyBinding annotation,
			TypeMappingAnnotationProcessorContext context) {
		org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBinder binder = createBinder( annotation.binder(), context );
		mapping.routingKeyBinder( binder );
	}

	private org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBinder createBinder(
			org.hibernate.search.mapper.pojo.bridge.mapping.annotation.RoutingKeyBinderRef binderReferenceAnnotation,
			MappingAnnotationProcessorContext context) {
		Optional<BeanReference<? extends org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBinder>> binderReference =
				context.toBeanReference(
						org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBinder.class,
						org.hibernate.search.mapper.pojo.bridge.mapping.annotation.RoutingKeyBinderRef.UndefinedBinderImplementationType.class,
						binderReferenceAnnotation.type(), binderReferenceAnnotation.name()
				);

		if ( !binderReference.isPresent() ) {
			throw log.missingBinderReferenceInBinding();
		}

		return new BeanDelegatingBinder( binderReference.get() );
	}
}
