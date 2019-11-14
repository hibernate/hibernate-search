/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.reporting.spi.FailureCollector;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.MarkerBinding;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.PropertyBinding;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.RoutingKeyBinding;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.TypeBinding;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.reporting.impl.PojoEventContexts;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.impl.GenericTypeContext;
import org.hibernate.search.util.common.reflect.impl.ReflectionUtils;

public class AnnotationProcessorProvider {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final BeanResolver beanResolver;
	private final FailureCollector rootFailureCollector;
	private final MappingAnnotationProcessorContext context;

	private final Map<Class<? extends Annotation>, Optional<BeanReference<? extends TypeMappingAnnotationProcessor>>>
			typeAnnotationProcessorReferenceCache = new HashMap<>();
	private final Map<Class<? extends Annotation>, Optional<BeanReference<? extends PropertyMappingAnnotationProcessor>>>
			propertyAnnotationProcessorReferenceCache = new HashMap<>();
	private final TypeMappingAnnotationProcessor<Annotation> typeBindingAnnotationProcessor = new TypeBridgeProcessor();
	private final TypeMappingAnnotationProcessor<Annotation> routingKeyBindingAnnotationProcessor = new RoutingKeyBridgeProcessor();
	private final PropertyMappingAnnotationProcessor<Annotation> propertyBindingAnnotationProcessor = new PropertyBridgeProcessor();
	private final PropertyMappingAnnotationProcessor<Annotation> markerBindingAnnotationProcessor = new MarkerProcessor();

	public AnnotationProcessorProvider(BeanResolver beanResolver, FailureCollector rootFailureCollector,
			MappingAnnotationProcessorContext context) {
		this.beanResolver = beanResolver;
		this.rootFailureCollector = rootFailureCollector;
		this.context = context;
	}

	@SuppressWarnings("unchecked") // Checked using reflection in createProcessorBean
	public <A extends Annotation> Optional<BeanHolder<? extends TypeMappingAnnotationProcessor<? super A>>>
			createTypeAnnotationProcessor(A annotation) {
		Class<? extends A> annotationType = (Class<? extends A>) annotation.annotationType();
		BeanHolder<? extends TypeMappingAnnotationProcessor<? super A>> processor = null;
		try {
			Optional<BeanReference<? extends TypeMappingAnnotationProcessor>> processorReference =
					getTypeAnnotationProcessorReference( annotationType );
			if ( !processorReference.isPresent() ) {
				return Optional.empty();
			}
			processor = (BeanHolder<? extends TypeMappingAnnotationProcessor<? super A>>) createProcessorBean(
					TypeMappingAnnotationProcessor.class, annotationType, processorReference.get()
			);
		}
		catch (RuntimeException e) {
			rootFailureCollector
					.withContext( PojoEventContexts.fromAnnotationType( annotationType ) )
					.add( e );
			// Ignore this annotation from now on.
			typeAnnotationProcessorReferenceCache.put( annotationType, Optional.empty() );
		}
		return Optional.ofNullable( processor );
	}

	@SuppressWarnings("unchecked") // Checked using reflection in createProcessorBean
	public <A extends Annotation> Optional<BeanHolder<? extends PropertyMappingAnnotationProcessor<? super A>>>
			createPropertyAnnotationProcessor(A annotation) {
		Class<? extends A> annotationType = (Class<? extends A>) annotation.annotationType();
		BeanHolder<? extends PropertyMappingAnnotationProcessor<? super A>> processor = null;
		try {
			Optional<BeanReference<? extends PropertyMappingAnnotationProcessor>> processorReference =
					getPropertyAnnotationProcessorReference( annotationType );
			if ( !processorReference.isPresent() ) {
				return Optional.empty();
			}
			processor = (BeanHolder<? extends PropertyMappingAnnotationProcessor<? super A>>) createProcessorBean(
					PropertyMappingAnnotationProcessor.class, annotationType, processorReference.get()
			);
		}
		catch (RuntimeException e) {
			rootFailureCollector
					.withContext( PojoEventContexts.fromAnnotationType( annotationType ) )
					.add( e );
			// Ignore this annotation from now on.
			typeAnnotationProcessorReferenceCache.put( annotationType, Optional.empty() );
		}
		return Optional.ofNullable( processor );
	}

	private Optional<BeanReference<? extends TypeMappingAnnotationProcessor>>
			getTypeAnnotationProcessorReference(Class<? extends Annotation> annotationType) {
		Optional<BeanReference<? extends TypeMappingAnnotationProcessor>> processorReference =
				typeAnnotationProcessorReferenceCache.get( annotationType );
		if ( processorReference == null ) { // We really mean to check for null here (missing key in the map), not isPresent().
			processorReference = createTypeAnnotationProcessorReference( annotationType );
			typeAnnotationProcessorReferenceCache.put( annotationType, processorReference );
		}
		return processorReference;
	}

	private Optional<BeanReference<? extends PropertyMappingAnnotationProcessor>>
			getPropertyAnnotationProcessorReference(Class<? extends Annotation> annotationType) {
		Optional<BeanReference<? extends PropertyMappingAnnotationProcessor>> processorReference =
				propertyAnnotationProcessorReferenceCache.get( annotationType );
		if ( processorReference == null ) { // We really mean to check for null here (missing key in the map), not isPresent().
			processorReference = createPropertyAnnotationProcessorReference( annotationType );
			propertyAnnotationProcessorReferenceCache.put( annotationType, processorReference );
		}
		return processorReference;
	}

	private Optional<BeanReference<? extends TypeMappingAnnotationProcessor>>
			createTypeAnnotationProcessorReference(Class<? extends Annotation> annotationType) {
		TypeMapping mapping = annotationType.getAnnotation( TypeMapping.class );
		if ( mapping == null ) {
			// TODO HSEARCH-3135 remove this when TypeMapping is used everywhere (tests, documentation)
			if ( annotationType.getAnnotation( TypeBinding.class ) != null ) {
				return Optional.of( BeanReference.ofInstance( typeBindingAnnotationProcessor ) );
			}
			if ( annotationType.getAnnotation( RoutingKeyBinding.class ) != null ) {
				return Optional.of( BeanReference.ofInstance( routingKeyBindingAnnotationProcessor ) );
			}
			// Not a type mapping annotation: ignore it.
			return Optional.empty();
		}

		TypeMappingAnnotationProcessorRef referenceAnnotation = mapping.processor();
		Optional<BeanReference<? extends TypeMappingAnnotationProcessor>> processorReference =
				context.toBeanReference(
						TypeMappingAnnotationProcessor.class,
						TypeMappingAnnotationProcessorRef.UndefinedProcessorImplementationType.class,
						referenceAnnotation.type(), referenceAnnotation.name()
				);
		if ( !processorReference.isPresent() ) {
			throw log.missingProcessorReferenceInMappingAnnotation( TypeMapping.class );
		}
		return processorReference;
	}

	private <A extends Annotation> Optional<BeanReference<? extends PropertyMappingAnnotationProcessor>>
			createPropertyAnnotationProcessorReference(Class<? extends A> annotationType) {
		PropertyMapping mapping = annotationType.getAnnotation( PropertyMapping.class );
		if ( mapping == null ) {
			// TODO HSEARCH-3135 remove this when PropertyMapping is used everywhere (tests, documentation)
			if ( annotationType.getAnnotation( PropertyBinding.class ) != null ) {
				return Optional.of( BeanReference.ofInstance( propertyBindingAnnotationProcessor ) );
			}
			if ( annotationType.getAnnotation( MarkerBinding.class ) != null ) {
				return Optional.of( BeanReference.ofInstance( markerBindingAnnotationProcessor ) );
			}
			// Not a property mapping annotation: ignore it.
			return Optional.empty();
		}

		PropertyMappingAnnotationProcessorRef referenceAnnotation = mapping.processor();
		Optional<BeanReference<? extends PropertyMappingAnnotationProcessor>> processorReference =
				context.toBeanReference(
						PropertyMappingAnnotationProcessor.class,
						PropertyMappingAnnotationProcessorRef.UndefinedProcessorImplementationType.class,
						referenceAnnotation.type(), referenceAnnotation.name()
				);
		if ( !processorReference.isPresent() ) {
			throw log.missingProcessorReferenceInMappingAnnotation( PropertyMapping.class );
		}
		return processorReference;
	}

	private <B, A extends Annotation> BeanHolder<? extends B> createProcessorBean(
			Class<B> expectedType, Class<A> encounteredAnnotationType,
			BeanReference<? extends B> processorReference) {
		BeanHolder<? extends B> delegateHolder = processorReference.resolve( beanResolver );
		try {
			B processor = delegateHolder.get();
			GenericTypeContext bridgeTypeContext = new GenericTypeContext( processor.getClass() );
			Class<?> processorAnnotationType = bridgeTypeContext.resolveTypeArgument( expectedType, 0 )
					.map( ReflectionUtils::getRawType )
					.orElseThrow( () -> new AssertionFailure(
							"Could not auto-detect the annotation type accepted by processor '"
									+ processor + "'."
									+ " There is a bug in Hibernate Search, please report it."
					) );
			if ( !processorAnnotationType.isAssignableFrom( encounteredAnnotationType ) ) {
				throw log.invalidAnnotationTypeForAnnotationProcessor( processor, processorAnnotationType );
			}

			return delegateHolder;
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e ).push( delegateHolder );
			throw e;
		}
	}

}
