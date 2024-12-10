/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.common.impl;

import java.util.Objects;

import org.hibernate.search.engine.backend.common.DocumentReference;

public class LuceneDocumentReference implements DocumentReference {

	private final String typeName;

	private final String id;

	public LuceneDocumentReference(String typeName, String id) {
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
	public boolean equals(Object obj) {
		if ( obj == null || obj.getClass() != getClass() ) {
			return false;
		}
		LuceneDocumentReference other = (LuceneDocumentReference) obj;
		return Objects.equals( typeName, other.typeName ) && Objects.equals( id, other.id );
	}

	@Override
	public int hashCode() {
		return Objects.hash( typeName, id );
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "typeName=" ).append( typeName )
				.append( ", id=" ).append( id )
				.append( "]" )
				.toString();
	}

}
