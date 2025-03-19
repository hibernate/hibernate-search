/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor.model.impl;

import static org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector.noPrefix;
import static org.hibernate.search.metamodel.processor.impl.ProcessorElementUtils.propertyElements;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoCaster;
import org.hibernate.search.mapper.pojo.model.spi.PojoConstructorModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.metamodel.processor.impl.HibernateSearchMetamodelProcessorContext;
import org.hibernate.search.metamodel.processor.mapping.impl.ProcessorPojoModelsBootstrapIntrospector;

public class ProcessorPojoRawTypeModel<T> implements PojoRawTypeModel<T> {

	private final TypeMirror typeMirror;
	private final TypeElement typeElement;
	private final HibernateSearchMetamodelProcessorContext context;
	private final ProcessorPojoModelsBootstrapIntrospector introspector;
	private final Map<String, ProcessorPojoPropertyModel<?>> propertyModels = new HashMap<>();

	public ProcessorPojoRawTypeModel(TypeElement typeElement, HibernateSearchMetamodelProcessorContext context,
			ProcessorPojoModelsBootstrapIntrospector introspector) {
		this( null, typeElement, context, introspector );
	}

	public ProcessorPojoRawTypeModel(TypeMirror typeMirror, HibernateSearchMetamodelProcessorContext context,
			ProcessorPojoModelsBootstrapIntrospector introspector) {
		this( typeMirror, (TypeElement) context.typeUtils().asElement( typeMirror ), context, introspector );
	}

	private ProcessorPojoRawTypeModel(TypeMirror typeMirror, TypeElement typeElement,
			HibernateSearchMetamodelProcessorContext context, ProcessorPojoModelsBootstrapIntrospector introspector) {
		this.typeMirror = typeMirror;
		if ( typeElement == null && typeMirror instanceof PrimitiveType primitiveType ) {
			this.typeElement = context.typeUtils().boxedClass( primitiveType );
		}
		else {
			this.typeElement = typeElement;
		}
		this.context = context;
		this.introspector = introspector;
	}

	@SuppressWarnings("unchecked")
	@Override
	public PojoRawTypeIdentifier<T> typeIdentifier() {
		if ( typeElement.getKind() == ElementKind.ENUM ) {
			return PojoRawTypeIdentifier.of( (Class<T>) EnumStub.class, typeElement.getQualifiedName().toString() );
		}
		return PojoRawTypeIdentifier.of( (Class<T>) TypeElement.class, typeElement.getQualifiedName().toString() );
	}

	// TODO: see `isSubTypeOf` if we can remove this dummy enum
	private enum EnumStub {
	}

	@Override
	public boolean isAbstract() {
		return typeElement.getModifiers().contains( Modifier.ABSTRACT );
	}

	@Override
	public boolean isSubTypeOf(MappableTypeModel otherModel) {
		TypeElement otherTypeElement;
		if ( otherModel instanceof ProcessorPojoRawTypeModel<?> other ) {
			otherTypeElement = other.typeElement;
		}
		else {
			otherTypeElement = context.elementUtils().getTypeElement( otherModel.name() );
		}
		//TODO handle enums differently here, doesn't look like it treats SomeEnum as subtype of Enum.
		return otherTypeElement != null
				&& ( context.typeUtils().isSameType( otherTypeElement.asType(), typeElement.asType() )
						|| context.typeUtils().isSubtype( typeElement.asType(), otherTypeElement.asType() ) );
	}

	@SuppressWarnings("unchecked")
	@Override
	public Stream<? extends PojoRawTypeModel<? super T>> ascendingSuperTypes() {
		return introspector.typeOrdering().ascendingSuperTypes( typeElement )
				.map( e -> (PojoRawTypeModel<? super T>) introspector.typeModel( e ) );
	}

	@SuppressWarnings("unchecked")
	@Override
	public Stream<? extends PojoRawTypeModel<? super T>> descendingSuperTypes() {
		return introspector.typeOrdering().descendingSuperTypes( typeElement )
				.map( e -> (PojoRawTypeModel<? super T>) introspector.typeModel( e ) );
	}

	@Override
	public Stream<? extends Annotation> annotations() {
		throw new UnsupportedOperationException();
	}

	@Override
	public PojoConstructorModel<T> mainConstructor() {
		throw new UnsupportedOperationException();
	}

	@Override
	public PojoConstructorModel<T> constructor(Class<?>... parameterTypes) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<PojoConstructorModel<T>> declaredConstructors() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<PojoPropertyModel<?>> declaredProperties() {
		return propertyElements( context.elementUtils(), typeElement )
				.map( this::propertyModel )
				.collect( Collectors.toList() );
	}

	@SuppressWarnings("unchecked")
	@Override
	public PojoTypeModel<? extends T> cast(PojoTypeModel<?> other) {
		return (PojoTypeModel<? extends T>) other;
	}

	@SuppressWarnings("unchecked")
	@Override
	public PojoCaster<T> caster() {
		return (PojoCaster<T>) ProcessorPojoCaster.INSTANCE;
	}

	@Override
	public String name() {
		return typeElement.getQualifiedName().toString();
	}

	@Override
	public PojoPropertyModel<?> property(String propertyName) {
		return propertyElements( context.elementUtils(), typeElement )
				.filter( element -> propertyName.equals( propertyName( element ) ) )
				.map( this::propertyModel )
				.findAny()
				.orElse( null );
	}

	private ProcessorPojoPropertyModel<?> propertyModel(Element element) {
		String propertyName = propertyName( element );
		if ( element.getKind() == ElementKind.FIELD ) {
			return propertyModels.computeIfAbsent( propertyName,
					k -> new ProcessorPojoPropertyModel<>( (VariableElement) element, propertyName, context, introspector ) );
		}
		if ( element.getKind() == ElementKind.METHOD ) {
			return propertyModels.computeIfAbsent( propertyName,
					k -> new ProcessorPojoPropertyModel<>( (ExecutableElement) element, propertyName, context, introspector ) );
		}
		throw new IllegalArgumentException( "Unsupported element kind: " + element.getKind() );
	}

	private static String propertyName(Element element) {
		if ( element.getKind() == ElementKind.FIELD ) {
			return element.getSimpleName().toString();
		}
		if ( element.getKind() == ElementKind.METHOD ) {
			return noPrefix( element.getSimpleName().toString() );
		}
		throw new IllegalArgumentException( "Unsupported element kind: " + element.getKind() );
	}

	@Override
	public <U> Optional<PojoTypeModel<? extends U>> castTo(Class<U> target) {
		return Optional.empty();
	}

	@Override
	public Optional<? extends PojoTypeModel<?>> typeArgument(Class<?> rawSuperType, int typeParameterIndex) {
		if ( typeMirror == null ) {
			return Optional.empty();
		}

		TypeElement rawSuperElement = context.elementUtils().getTypeElement( rawSuperType.getName() );
		if ( rawSuperElement == null ) {
			return Optional.empty();
		}

		return Optional.ofNullable(
				typeArgument( typeMirror, context.typeUtils().erasure( rawSuperElement.asType() ), typeParameterIndex ) )
				.map( introspector::typeModel );
	}

	private TypeMirror typeArgument(TypeMirror current, TypeMirror rawSuperType, int typeParameterIndex) {
		if ( current == null || current.getKind() == TypeKind.NONE ) {
			return null;
		}
		if ( current instanceof DeclaredType declaredType ) {
			if ( context.typeUtils().isSameType( context.typeUtils().erasure( current ), rawSuperType ) ) {
				return declaredType.getTypeArguments().get( typeParameterIndex );
			}

			TypeElement element = (TypeElement) declaredType.asElement();
			for ( TypeMirror mirror : element.getInterfaces() ) {
				TypeMirror argument = typeArgument( mirror, rawSuperType, typeParameterIndex );
				if ( argument != null ) {
					Name name = context.typeUtils().asElement( argument ).getSimpleName();
					for ( int i = 0; i < element.getTypeParameters().size(); i++ ) {
						if ( element.getTypeParameters().get( i ).getSimpleName().equals( name ) ) {
							return declaredType.getTypeArguments().get( i );
						}
					}
					return argument;
				}
			}
		}
		return null;
	}

	@Override
	public Optional<? extends PojoTypeModel<?>> arrayElementType() {
		return Optional.empty();
	}

	private static class ProcessorPojoCaster<T> implements PojoCaster<T> {

		static ProcessorPojoCaster<?> INSTANCE = new ProcessorPojoCaster<>();

		@Override
		public T cast(Object object) {
			throw new UnsupportedOperationException();
		}

		@Override
		public T castOrNull(Object object) {
			throw new UnsupportedOperationException();
		}
	}
}
