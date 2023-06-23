/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MappingAnnotatedProperty;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.reporting.spi.PojoEventContexts;
import org.hibernate.search.util.common.reflect.spi.AnnotationHelper;
import org.hibernate.search.util.common.reporting.EventContext;

public class PropertyMappingAnnotationProcessorContextImpl
		extends AbstractMappingAnnotationProcessorContext
		implements PropertyMappingAnnotationProcessorContext, MappingAnnotatedProperty {
	private final PojoRawTypeModel<?> typeModel;
	private final PojoPropertyModel<?> propertyModel;
	private final Annotation annotation;
	private final PojoMappingConfigurationContext configurationContext;

	public PropertyMappingAnnotationProcessorContextImpl(PojoRawTypeModel<?> typeModel, PojoPropertyModel<?> propertyModel,
			Annotation annotation,
			AnnotationHelper annotationHelper, PojoMappingConfigurationContext configurationContext) {
		super( annotationHelper );
		this.typeModel = typeModel;
		this.propertyModel = propertyModel;
		this.annotation = annotation;
		this.configurationContext = configurationContext;
	}

	@Override
	public MappingAnnotatedProperty annotatedElement() {
		return this; // Not a lot to implement, so we just implement everything in the same class
	}

	@Override
	public EventContext eventContext() {
		return PojoEventContexts.fromType( typeModel )
				.append( PojoEventContexts.fromPath( PojoModelPath.ofProperty( propertyModel.name() ) ) )
				.append( PojoEventContexts.fromAnnotation( annotation ) );
	}

	@Override
	public Class<?> javaClass() {
		return propertyModel.typeModel().rawType().typeIdentifier().javaClass();
	}

	@Override
	public Optional<Class<?>> javaClass(ContainerExtractorPath extractorPath) {
		return configurationContext.extractedValueType( propertyModel.typeModel(), extractorPath )
				.map( type -> type.rawType().typeIdentifier().javaClass() );
	}

	@Override
	public String name() {
		return propertyModel.name();
	}

	@Override
	public Stream<Annotation> allAnnotations() {
		return propertyModel.annotations().flatMap( annotationHelper::expandRepeatableContainingAnnotation );
	}

}
