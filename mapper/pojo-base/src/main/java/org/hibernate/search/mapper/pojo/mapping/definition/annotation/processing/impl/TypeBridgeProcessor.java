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
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.TypeBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.TypeBinding;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.AnnotationInitializingBeanDelegatingBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class TypeBridgeProcessor extends TypeAnnotationProcessor<Annotation> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public Stream<? extends Annotation> extractAnnotations(PojoRawTypeModel<?> typeModel) {
		return typeModel.getAnnotationsByMetaAnnotationType( TypeBinding.class );
	}

	@Override
	public void process(TypeMappingStep mappingContext, Annotation annotation,
			TypeMappingAnnotationProcessorContext context) {
		TypeBinder<?> binder = createTypeBinder( annotation, context );
		mappingContext.binder( binder );
	}

	private <A extends Annotation> TypeBinder createTypeBinder(A annotation, MappingAnnotationProcessorContext context) {
		TypeBinding bridgeMapping = annotation.annotationType().getAnnotation( TypeBinding.class );
		TypeBinderRef bridgeReferenceAnnotation = bridgeMapping.binder();
		Optional<BeanReference<? extends TypeBinder>> binderReference = context.toBeanReference(
				TypeBinder.class,
				TypeBinderRef.UndefinedBinderImplementationType.class,
				bridgeReferenceAnnotation.type(), bridgeReferenceAnnotation.name()
		);

		if ( !binderReference.isPresent() ) {
			throw log.missingBinderReferenceInBridgeMapping(
					bridgeMapping.annotationType(), annotation.annotationType()
			);
		}

		TypeBinder<A> binder = new AnnotationInitializingBeanDelegatingBinder<>( binderReference.get() );
		binder.initialize( annotation );
		return binder;
	}
}
