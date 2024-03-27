/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import java.util.Objects;

import org.hibernate.search.engine.common.EntityReference;

public final class StubEntityReference implements EntityReference {

	private final Class<?> type;

	private final String name;

	private final Object id;

	public StubEntityReference(Class<?> type, String name, Object id) {
		this.type = type;
		this.name = name;
		this.id = id;
	}

	@Override
	public Class<?> type() {
		return type;
	}

	public String name() {
		return name;
	}

	public Object id() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		if ( !( obj instanceof EntityReference ) ) {
			return false;
		}
		EntityReference other = (EntityReference) obj;
		return name.equals( other.name() ) && Objects.equals( id, other.id() );
	}

	@Override
	public int hashCode() {
		return Objects.hash( name, id );
	}

	@Override
	public String toString() {
		return name + "#" + id + " (" + type + ")";
	}

}
