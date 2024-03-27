/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.spi;

import java.util.Objects;

import org.hibernate.search.util.common.impl.Contracts;

/**
 * An identifier for POJO types.
 * <p>
 * On contrary to type models, type identifiers can be manipulated at runtime (after bootstrap),
 * but they do not provide any reflection capabilities.
 *
 * @see PojoRawTypeModel
 */
public final class PojoRawTypeIdentifier<T> {

	public static <T> PojoRawTypeIdentifier<T> of(Class<T> javaClass) {
		return new PojoRawTypeIdentifier<>( javaClass, null );
	}

	public static <T> PojoRawTypeIdentifier<T> of(Class<T> javaClass, String label) {
		return new PojoRawTypeIdentifier<>( javaClass, label );
	}

	private final Class<T> javaClass;
	private final String name;

	private PojoRawTypeIdentifier(Class<T> javaClass, String name) {
		Contracts.assertNotNull( javaClass, "javaClass" );
		this.javaClass = javaClass;
		this.name = name;
	}

	@Override
	public String toString() {
		if ( name == null ) {
			return javaClass.getName();
		}
		else {
			return name + " (" + javaClass.getName() + ")";
		}
	}

	@Override
	public boolean equals(Object obj) {
		if ( !( obj instanceof PojoRawTypeIdentifier ) ) {
			return false;
		}
		PojoRawTypeIdentifier<?> other = (PojoRawTypeIdentifier<?>) obj;
		return javaClass.equals( other.javaClass )
				&& Objects.equals( name, other.name );
	}

	@Override
	public int hashCode() {
		return Objects.hash( javaClass, name );
	}

	/**
	 * @return The exact Java {@link Class} for this type.
	 */
	public Class<T> javaClass() {
		return javaClass;
	}

	public boolean isNamed() {
		return name != null;
	}
}
