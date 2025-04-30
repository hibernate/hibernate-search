/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.processor.model.impl;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.lang.model.element.ElementKind;

import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.IdentifierBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelValueElement;
import org.hibernate.search.mapper.pojo.model.spi.PojoCaster;
import org.hibernate.search.mapper.pojo.model.spi.PojoConstructorModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.common.annotation.impl.SuppressJQAssistant;

public enum HibernateSearchProcessorEnum {
	;

	public static final Binder BINDER = new Binder();
	public static final Model MODEL = new Model();

	@SuppressJQAssistant(reason = "Need to cast to an impl type to get access to not-yet exposed method")
	public static class Binder implements ValueBinder, IdentifierBinder {

		@Override
		public void bind(IdentifierBindingContext<?> context) {
			if ( context.bridgedElement() instanceof PojoModelValueElement<?> element ) {
				if ( element.typeModel() instanceof ProcessorPojoRawTypeModel<?> pr ) {
					context.bridge( Object.class, new Bridge( pr.name() ) );
				}
			}
		}

		@Override
		public void bind(ValueBindingContext<?> context) {
			if ( context.bridgedElement() instanceof PojoModelValueElement<?> element ) {
				if ( element.typeModel() instanceof ProcessorPojoRawTypeModel<?> pr ) {
					context.bridge( Object.class, new Bridge( pr.name() ) );
				}
			}
		}
	}

	public static class Model implements PojoRawTypeModel<HibernateSearchProcessorEnum> {

		@Override
		public PojoRawTypeIdentifier<HibernateSearchProcessorEnum> typeIdentifier() {
			return PojoRawTypeIdentifier.of( HibernateSearchProcessorEnum.class );
		}

		@Override
		public boolean isAbstract() {
			return false;
		}

		@Override
		public boolean isSubTypeOf(MappableTypeModel otherModel) {
			if ( otherModel instanceof ProcessorPojoRawTypeModel<?> other ) {
				return other.typeElement().getKind() == ElementKind.ENUM;
			}
			return false;
		}

		@Override
		public Stream<? extends PojoRawTypeModel<? super HibernateSearchProcessorEnum>> ascendingSuperTypes() {
			return Stream.empty();
		}

		@Override
		public Stream<? extends PojoRawTypeModel<? super HibernateSearchProcessorEnum>> descendingSuperTypes() {
			return Stream.empty();
		}

		@Override
		public Stream<? extends Annotation> annotations() {
			throw new UnsupportedOperationException();
		}

		@Override
		public PojoConstructorModel<HibernateSearchProcessorEnum> mainConstructor() {
			throw new UnsupportedOperationException();
		}

		@Override
		public PojoConstructorModel<HibernateSearchProcessorEnum> constructor(Class<?>... parameterTypes) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Collection<PojoConstructorModel<HibernateSearchProcessorEnum>> declaredConstructors() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Collection<PojoPropertyModel<?>> declaredProperties() {
			return List.of();
		}

		@SuppressWarnings("unchecked")
		@Override
		public PojoTypeModel<? extends HibernateSearchProcessorEnum> cast(PojoTypeModel<?> other) {
			return (PojoTypeModel<? extends HibernateSearchProcessorEnum>) other;
		}

		@Override
		public PojoCaster<HibernateSearchProcessorEnum> caster() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String name() {
			return HibernateSearchProcessorEnum.class.getSimpleName();
		}

		@Override
		public PojoPropertyModel<?> property(String propertyName) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <U> Optional<PojoTypeModel<? extends U>> castTo(Class<U> target) {
			return Optional.empty();
		}

		@Override
		public Optional<? extends PojoTypeModel<?>> typeArgument(Class<?> rawSuperType, int typeParameterIndex) {
			return Optional.empty();
		}

		@Override
		public Optional<? extends PojoTypeModel<?>> arrayElementType() {
			return Optional.empty();
		}
	}

	public record Bridge(String valueType) implements ValueBridge<Object, String>, IdentifierBridge<Object> {

		@Override
		public String toIndexedValue(Object value, ValueBridgeToIndexedValueContext context) {
			return "";
		}

		@Override
		public void close() {
			// nothing to do here, but have to override as both interfaces have a default
		}

		@Override
		public String toDocumentIdentifier(Object propertyValue, IdentifierBridgeToDocumentIdentifierContext context) {
			return "";
		}

		@Override
		public Object fromDocumentIdentifier(String documentIdentifier, IdentifierBridgeFromDocumentIdentifierContext context) {
			return null;
		}
	}
}
