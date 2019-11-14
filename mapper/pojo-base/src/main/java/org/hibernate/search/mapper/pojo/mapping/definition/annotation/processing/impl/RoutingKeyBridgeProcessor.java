/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.stream.Stream;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.RoutingKeyBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.RoutingKeyBinding;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.AnnotationInitializingBeanDelegatingBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBinder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.spi.AnnotationHelper;

class RoutingKeyBridgeProcessor extends TypeAnnotationProcessor<Annotation> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public Stream<? extends Annotation> extractAnnotations(Stream<Annotation> annotations,
			AnnotationHelper annotationHelper) {
		return extractByMetaAnnotationType( annotations, annotationHelper, RoutingKeyBinding.class );
	}

	@Override
	public void process(TypeMappingStep mappingContext, Annotation annotation,
			TypeMappingAnnotationProcessorContext context) {
		RoutingKeyBinder<?> binder = createRoutingKeyBinder( annotation, context );
		mappingContext.routingKeyBinder( binder );
	}

	private <A extends Annotation> RoutingKeyBinder createRoutingKeyBinder(A annotation,
			MappingAnnotationProcessorContext context) {
		RoutingKeyBinding bridgeMapping = annotation.annotationType().getAnnotation( RoutingKeyBinding.class );
		RoutingKeyBinderRef bridgeReferenceAnnotation = bridgeMapping.binder();
		Optional<BeanReference<? extends RoutingKeyBinder>> binderReference = context.toBeanReference(
				RoutingKeyBinder.class,
				RoutingKeyBinderRef.UndefinedBinderImplementationType.class,
				bridgeReferenceAnnotation.type(), bridgeReferenceAnnotation.name()
		);

		if ( !binderReference.isPresent() ) {
			throw log.missingBinderReferenceInBridgeMapping(
					bridgeMapping.annotationType(), annotation.annotationType()
			);
		}

		RoutingKeyBinder<A> binder =
				new AnnotationInitializingBeanDelegatingBinder<>( binderReference.get() );
		binder.initialize( annotation );
		return binder;
	}
}
