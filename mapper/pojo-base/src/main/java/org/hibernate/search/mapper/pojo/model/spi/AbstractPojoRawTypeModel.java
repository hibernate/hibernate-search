/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.spi;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public abstract class AbstractPojoRawTypeModel<T, I extends PojoBootstrapIntrospector> implements PojoRawTypeModel<T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final I introspector;
	protected final PojoRawTypeIdentifier<T> typeIdentifier;
	private final PojoCaster<T> caster;

	private final Map<String, PojoPropertyModel<?>> propertyModelCache = new HashMap<>();

	private List<PojoPropertyModel<?>> declaredProperties;
	private List<PojoConstructorModel<T>> declaredConstructors;

	public AbstractPojoRawTypeModel(I introspector, PojoRawTypeIdentifier<T> typeIdentifier) {
		this.introspector = introspector;
		this.typeIdentifier = typeIdentifier;
		this.caster = new JavaClassPojoCaster<>( typeIdentifier.javaClass() );
	}

	@Override
	public final boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		AbstractPojoRawTypeModel<?, ?> that = (AbstractPojoRawTypeModel<?, ?>) o;
		/*
		 * We need to take the introspector into account, so that the engine does not confuse
		 * type models from different mappers during bootstrap.
		 */
		return Objects.equals( introspector, that.introspector )
				&& Objects.equals( typeIdentifier, that.typeIdentifier );
	}

	@Override
	public final int hashCode() {
		return Objects.hash( introspector, typeIdentifier );
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName() + "[" + typeIdentifier + "]";
	}

	@Override
	public final PojoRawTypeIdentifier<T> typeIdentifier() {
		return typeIdentifier;
	}

	@Override
	public final String name() {
		return typeIdentifier.toString();
	}

	@Override
	public final PojoConstructorModel<T> mainConstructor() {
		Collection<PojoConstructorModel<T>> theDeclaredConstructors = declaredConstructors();
		if ( theDeclaredConstructors.size() != 1 ) {
			throw log.cannotFindMainConstructorNotExactlyOneConstructor( this );
		}
		return theDeclaredConstructors.iterator().next();
	}

	@Override
	public final PojoConstructorModel<T> constructor(Class<?>... parameterTypes) {
		for ( PojoConstructorModel<T> constructor : declaredConstructors() ) {
			if ( Arrays.equals( parameterTypes, constructor.parametersJavaTypes() ) ) {
				return constructor;
			}
		}
		throw log.cannotFindConstructorWithParameterTypes( this, parameterTypes, declaredConstructors() );
	}

	@Override
	public Collection<PojoConstructorModel<T>> declaredConstructors() {
		if ( declaredConstructors == null ) {
			declaredConstructors = createDeclaredConstructors();
		}
		return declaredConstructors;
	}

	protected abstract List<PojoConstructorModel<T>> createDeclaredConstructors();

	@Override
	public final PojoPropertyModel<?> property(String propertyName) {
		PojoPropertyModel<?> propertyModel = propertyOrNull( propertyName );
		if ( propertyModel == null ) {
			throw log.cannotFindReadableProperty( this, propertyName );
		}
		return propertyModel;
	}

	@Override
	public final Collection<PojoPropertyModel<?>> declaredProperties() {
		if ( declaredProperties == null ) {
			declaredProperties = Collections.unmodifiableList( declaredPropertyNames()
					.map( this::propertyOrNull )
					.filter( Objects::nonNull )
					.collect( Collectors.toList() ) );
		}
		return declaredProperties;
	}

	@Override
	@SuppressWarnings("unchecked")
	public PojoTypeModel<? extends T> cast(PojoTypeModel<?> other) {
		if ( other.rawType().isSubTypeOf( this ) ) {
			// Redundant cast; no need to create a new type.
			return (PojoTypeModel<? extends T>) other;
		}
		else {
			return doCast( other );
		}
	}

	protected PojoTypeModel<? extends T> doCast(PojoTypeModel<?> other) {
		return other.castTo( typeIdentifier.javaClass() ).orElse( this );
	}

	@Override
	public final PojoCaster<T> caster() {
		return caster;
	}

	@Override
	public <U> Optional<PojoTypeModel<? extends U>> castTo(Class<U> target) {
		// Let the caller decide of the result:
		// we don't have any generics information to add to the resulting type.
		return Optional.empty();
	}

	protected abstract Stream<String> declaredPropertyNames();

	protected abstract PojoPropertyModel<?> createPropertyModel(String propertyName);

	private PojoPropertyModel<?> propertyOrNull(String propertyName) {
		return propertyModelCache.computeIfAbsent( propertyName, this::createPropertyModel );
	}

}
