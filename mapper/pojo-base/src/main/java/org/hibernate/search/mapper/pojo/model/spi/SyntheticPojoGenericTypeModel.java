/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.spi;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.hibernate.search.util.common.impl.CollectionHelper;

/**
 * A synthetic implementation of {@link PojoTypeModel},
 * i.e. one that is not bound to an actual Java type, but simulates one.
 */
public final class SyntheticPojoGenericTypeModel<T> extends AbstractPojoGenericTypeModel<T> {

	public static <T> PojoTypeModel<T[]> array(PojoRawTypeModel<? super T[]> rawTypeModel,
			PojoTypeModel<T> elementType) {
		return new SyntheticPojoGenericTypeModel<>( rawTypeModel, elementType, Collections.emptyList() );
	}

	public static <T> PojoTypeModel<T> genericType(PojoRawTypeModel<? super T> rawTypeModel,
			PojoTypeModel<?> firstTypeArgument, PojoTypeModel<?>... otherTypeArguments) {
		return new SyntheticPojoGenericTypeModel<>( rawTypeModel, null,
				CollectionHelper.asList( firstTypeArgument, otherTypeArguments ) );
	}

	// For types that should not report implementing an interface, even though they do
	// Example: dynamic Map types, that should not be treated as maps when it comes to container extractors.
	public static <T> PojoTypeModel<T> opaqueType(PojoRawTypeModel<T> rawTypeModel) {
		return new SyntheticPojoGenericTypeModel<>( rawTypeModel, null, Collections.emptyList() );
	}

	private final PojoTypeModel<?> arrayElementType;
	private final List<PojoTypeModel<?>> genericTypeArguments;

	private SyntheticPojoGenericTypeModel(PojoRawTypeModel<? super T> rawTypeModel,
			PojoTypeModel<?> arrayElementType,
			List<PojoTypeModel<?>> genericTypeArguments) {
		super( rawTypeModel );
		this.arrayElementType = arrayElementType;
		this.genericTypeArguments = genericTypeArguments;
	}

	@Override
	public String name() {
		if ( arrayElementType != null ) {
			return arrayElementType.name() + "[]";
		}

		if ( genericTypeArguments.isEmpty() ) {
			return rawType().name();
		}

		StringBuilder builder = new StringBuilder();
		builder.append( rawType().name() );
		builder.append( '<' );
		boolean first = true;
		for ( PojoTypeModel<?> genericTypeArgument : genericTypeArguments ) {
			if ( first ) {
				first = false;
			}
			else {
				builder.append( ", " );
			}
			builder.append( genericTypeArgument.name() );
		}
		builder.append( '>' );
		return builder.toString();
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		SyntheticPojoGenericTypeModel<?> that = (SyntheticPojoGenericTypeModel<?>) o;
		return Objects.equals( rawType(), that.rawType() )
				&& Objects.equals( arrayElementType, that.arrayElementType )
				&& Objects.equals( genericTypeArguments, that.genericTypeArguments );
	}

	@Override
	public int hashCode() {
		return Objects.hash( rawType(), arrayElementType, genericTypeArguments );
	}

	@Override
	public <U> Optional<PojoTypeModel<? extends U>> castTo(Class<U> target) {
		// Cannot cast synthetic types.
		return Optional.empty();
	}

	@Override
	public Optional<PojoTypeModel<?>> arrayElementType() {
		return Optional.ofNullable( arrayElementType );
	}

	@Override
	public Optional<? extends PojoTypeModel<?>> typeArgument(Class<?> rawSuperType,
			int typeParameterIndex) {
		if ( genericTypeArguments.isEmpty() ) {
			// Raw type
			return Optional.empty();
		}
		if ( rawSuperType.isAssignableFrom( rawType().typeIdentifier().javaClass() ) ) {
			return Optional.of( genericTypeArguments.get( typeParameterIndex ) );
		}
		return Optional.empty();
	}
}
