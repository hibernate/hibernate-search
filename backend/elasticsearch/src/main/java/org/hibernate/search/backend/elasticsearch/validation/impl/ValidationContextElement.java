/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.validation.impl;

import java.util.Objects;

final class ValidationContextElement {

	private final ValidationContextType type;
	private final String name;

	public ValidationContextElement(ValidationContextType type, String name) {
		super();
		this.type = type;
		this.name = name;
	}

	public ValidationContextType getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return new StringBuilder()
				.append( type )
				.append( "[" )
				.append( name )
				.append( "]" )
				.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj != null && getClass().equals( obj.getClass() ) ) {
			ValidationContextElement other = (ValidationContextElement) obj;
			return Objects.equals( type, other.type )
					&& Objects.equals( name, other.name );
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Objects.hashCode( type );
		result = prime * result + Objects.hashCode( name );
		return result;
	}
}
