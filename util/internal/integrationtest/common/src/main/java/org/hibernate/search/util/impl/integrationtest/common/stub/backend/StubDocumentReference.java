/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend;

import java.util.Objects;

import org.hibernate.search.engine.backend.common.DocumentReference;

public class StubDocumentReference implements DocumentReference {

	private final String typeName;
	private final String id;

	public StubDocumentReference(String typeName, String id) {
		this.typeName = typeName;
		this.id = id;
	}

	@Override
	public String typeName() {
		return typeName;
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" +
				"typeName='" + typeName +
				", id=" + id +
				"]";
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		StubDocumentReference that = (StubDocumentReference) o;
		return Objects.equals( typeName, that.typeName )
				&& Objects.equals( id, that.id );
	}

	@Override
	public int hashCode() {
		return Objects.hash( typeName, id );
	}
}
