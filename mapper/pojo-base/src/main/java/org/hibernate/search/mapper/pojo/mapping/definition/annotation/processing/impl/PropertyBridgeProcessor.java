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
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class PropertyBridgeProcessor extends PropertyAnnotationProcessor<Annotation> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	PropertyBridgeProcessor(AnnotationProcessorHelper helper) {
		super( helper );
	}

	@Override
	public Stream<? extends Annotation> extractAnnotations(PojoPropertyModel<?> propertyModel) {
		return propertyModel.getAnnotationsByMetaAnnotationType( PropertyBinding.class );
	}

	@Override
	public void process(PropertyMappingStep mappingContext,
			PojoRawTypeModel<?> typeModel, PojoPropertyModel<?> propertyModel,
			Annotation annotation) {
		PropertyBinder<?> binder = createPropertyBinder( annotation );
		mappingContext.binder( binder );
	}

	private <A extends Annotation> PropertyBinder createPropertyBinder(A annotation) {
		PropertyBinding bridgeMapping = annotation.annotationType().getAnnotation( PropertyBinding.class );
		PropertyBinderRef bridgeReferenceAnnotation = bridgeMapping.binder();
		Optional<BeanReference<? extends PropertyBinder>> binderReference = helper.toBeanReference(
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
