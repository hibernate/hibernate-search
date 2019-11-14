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
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.PropertyBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.PropertyBinding;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.AnnotationInitializingBeanDelegatingBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.spi.AnnotationHelper;

class PropertyBridgeProcessor extends PropertyAnnotationProcessor<Annotation> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public Stream<? extends Annotation> extractAnnotations(Stream<Annotation> annotations,
			AnnotationHelper annotationHelper) {
		return extractByMetaAnnotationType( annotations, annotationHelper, PropertyBinding.class );
	}

	@Override
	public void process(PropertyMappingStep mappingContext, Annotation annotation,
			PropertyMappingAnnotationProcessorContext context) {
		PropertyBinder<?> binder = createPropertyBinder( annotation, context );
		mappingContext.binder( binder );
	}

	private <A extends Annotation> PropertyBinder createPropertyBinder(A annotation, MappingAnnotationProcessorContext context) {
		PropertyBinding bridgeMapping = annotation.annotationType().getAnnotation( PropertyBinding.class );
		PropertyBinderRef bridgeReferenceAnnotation = bridgeMapping.binder();
		Optional<BeanReference<? extends PropertyBinder>> binderReference = context.toBeanReference(
				PropertyBinder.class,
				PropertyBinderRef.UndefinedBinderImplementationType.class,
				bridgeReferenceAnnotation.type(), bridgeReferenceAnnotation.name()
		);

		if ( !binderReference.isPresent() ) {
			throw log.missingBinderReferenceInBridgeMapping(
					bridgeMapping.annotationType(), annotation.annotationType()
			);
		}

		PropertyBinder<A> binder =
				new AnnotationInitializingBeanDelegatingBinder<>( binderReference.get() );
		binder.initialize( annotation );
		return binder;
	}
}
