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
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.MarkerBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.MarkerBinding;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.AnnotationInitializingBeanDelegatingBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBinder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class MarkerProcessor extends PropertyAnnotationProcessor<Annotation> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public Stream<? extends Annotation> extractAnnotations(PojoPropertyModel<?> propertyModel) {
		return propertyModel.getAnnotationsByMetaAnnotationType( MarkerBinding.class );
	}

	@Override
	public void process(PropertyMappingStep mappingContext, Annotation annotation,
			PropertyMappingAnnotationProcessorContext context) {
		MarkerBinder<?> binder = createMarkerBinder( annotation, context );
		mappingContext.marker( binder );
	}

	private <A extends Annotation> MarkerBinder createMarkerBinder(A annotation, MappingAnnotationProcessorContext context) {
		MarkerBinding markerBinding = annotation.annotationType().getAnnotation( MarkerBinding.class );
		MarkerBinderRef binderReferenceAnnotation = markerBinding.binder();
		Optional<BeanReference<? extends MarkerBinder>> binderReference = context.toBeanReference(
				MarkerBinder.class,
				MarkerBinderRef.UndefinedBinderImplementationType.class,
				binderReferenceAnnotation.type(), binderReferenceAnnotation.name()
		);

		if ( !binderReference.isPresent() ) {
			throw log.missingBinderReferenceInMarkerMapping(
					MarkerBinding.class, annotation.annotationType()
			);
		}

		MarkerBinder<A> binder = new AnnotationInitializingBeanDelegatingBinder<>( binderReference.get() );
		binder.initialize( annotation );
		return binder;
	}
}
