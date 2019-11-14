/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.MarkerBinding;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.PropertyBinding;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.RoutingKeyBinding;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.TypeBinding;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.NonStandardField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ScaledNumberField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessor;
import org.hibernate.search.util.common.reflect.spi.AnnotationHelper;

public class AnnotationProcessorProvider {

	private final AnnotationHelper annotationHelper;

	private final Map<Class<? extends Annotation>, TypeMappingAnnotationProcessor<? extends Annotation>> simpleTypeAnnotationProcessors = new HashMap<>();
	private final Map<Class<? extends Annotation>, PropertyMappingAnnotationProcessor<? extends Annotation>> simplePropertyAnnotationProcessors = new HashMap<>();
	private final TypeMappingAnnotationProcessor<Annotation> typeBindingAnnotationProcessor = new TypeBridgeProcessor();
	private final TypeMappingAnnotationProcessor<Annotation> routingKeyBindingAnnotationProcessor = new RoutingKeyBridgeProcessor();
	private final PropertyMappingAnnotationProcessor<Annotation> propertyBindingAnnotationProcessor = new PropertyBridgeProcessor();
	private final PropertyMappingAnnotationProcessor<Annotation> markerBindingAnnotationProcessor = new MarkerProcessor();

	public AnnotationProcessorProvider(AnnotationHelper annotationHelper) {
		this.annotationHelper = annotationHelper;

		simpleTypeAnnotationProcessors.put( Indexed.class, new IndexedProcessor() );

		simplePropertyAnnotationProcessors.put( AssociationInverseSide.class, new AssociationInverseSideProcessor() );
		simplePropertyAnnotationProcessors.put( IndexingDependency.class, new IndexingDependencyProcessor() );
		simplePropertyAnnotationProcessors.put( DocumentId.class, new DocumentIdProcessor() );
		simplePropertyAnnotationProcessors.put( GenericField.class, new GenericFieldProcessor() );
		simplePropertyAnnotationProcessors.put( FullTextField.class, new FullTextFieldProcessor() );
		simplePropertyAnnotationProcessors.put( KeywordField.class, new KeywordFieldProcessor() );
		simplePropertyAnnotationProcessors.put( ScaledNumberField.class, new ScaledNumberFieldProcessor() );
		simplePropertyAnnotationProcessors.put( NonStandardField.class, new NonStandardFieldProcessor() );
		simplePropertyAnnotationProcessors.put( IndexedEmbedded.class, new IndexedEmbeddedProcessor() );
	}

	@SuppressWarnings("unchecked") // We take care to build the map with matching types
	public <A extends Annotation> Optional<TypeMappingAnnotationProcessor<? super A>> getTypeMappingAnnotationProcessor(A annotation) {
		TypeMappingAnnotationProcessor<? extends Annotation> result = simpleTypeAnnotationProcessors.get( annotation.annotationType() );
		if ( result != null ) {
			return Optional.of( (TypeMappingAnnotationProcessor<? super A>) result );
		}
		if ( annotationHelper.isMetaAnnotated( annotation, TypeBinding.class ) ) {
			return Optional.of( typeBindingAnnotationProcessor );
		}
		if ( annotationHelper.isMetaAnnotated( annotation, RoutingKeyBinding.class ) ) {
			return Optional.of( routingKeyBindingAnnotationProcessor );
		}
		return Optional.empty();
	}

	@SuppressWarnings("unchecked") // We take care to build the map with matching types
	public <A extends Annotation> Optional<PropertyMappingAnnotationProcessor<? super A>> getPropertyMappingAnnotationProcessor(A annotation) {
		PropertyMappingAnnotationProcessor<? extends Annotation> result = simplePropertyAnnotationProcessors.get( annotation.annotationType() );
		if ( result != null ) {
			return Optional.of( (PropertyMappingAnnotationProcessor<? super A>) result );
		}
		if ( annotationHelper.isMetaAnnotated( annotation, PropertyBinding.class ) ) {
			return Optional.of( propertyBindingAnnotationProcessor );
		}
		if ( annotationHelper.isMetaAnnotated( annotation, MarkerBinding.class ) ) {
			return Optional.of( markerBindingAnnotationProcessor );
		}
		return Optional.empty();
	}

}
