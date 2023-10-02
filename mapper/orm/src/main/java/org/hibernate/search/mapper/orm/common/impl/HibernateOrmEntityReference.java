/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.common.impl;

import java.util.Objects;

import org.hibernate.search.mapper.pojo.common.spi.PojoEntityReference;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

/**
 * The (legacy) EntityReference implementation specific to the Hibernate ORM mapper.
 *
 * @deprecated Wherever possible, use {@link PojoEntityReference} instead.
 * This may not be possible everywhere due to backwards compatibility constraints.
 */
@Deprecated
public final class HibernateOrmEntityReference implements org.hibernate.search.mapper.orm.common.EntityReference {

	public static org.hibernate.search.mapper.orm.common.EntityReference withDefaultName(Class<?> type, Object id) {
		return withName( type, type.getSimpleName(), id );
	}

	public static org.hibernate.search.mapper.orm.common.EntityReference withName(Class<?> type, String name, Object id) {
		return new HibernateOrmEntityReference( PojoRawTypeIdentifier.of( type ), name, id );
	}

	private final PojoRawTypeIdentifier<?> typeIdentifier;

	private final String name;

	private final Object id;

	public HibernateOrmEntityReference(PojoRawTypeIdentifier<?> typeIdentifier, String name, Object id) {
		this.typeIdentifier = typeIdentifier;
		this.name = name;
		this.id = id;
	}

	@Override
	public Class<?> type() {
		return typeIdentifier.javaClass();
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public Object id() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		if ( !( obj instanceof org.hibernate.search.engine.common.EntityReference ) ) {
			return false;
		}
		org.hibernate.search.engine.common.EntityReference other = (org.hibernate.search.engine.common.EntityReference) obj;
		return name.equals( other.name() ) && Objects.equals( id, other.id() );
	}

	@Override
	public int hashCode() {
		return Objects.hash( name, id );
	}

	@Override
	public String toString() {
		// Apparently this is the usual format for references to Hibernate ORM entities.
		// Let's use the same format here, even if we're not using Hibernate ORM: it's good enough.
		return name + "#" + id;
	}

}
