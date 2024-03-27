/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.processing.building.impl;

import java.util.Objects;

import org.hibernate.search.engine.mapper.model.spi.MappingElement;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.reporting.spi.PojoEventContexts;
import org.hibernate.search.util.common.reporting.EventContext;

public class PojoIndexedEmbeddedMappingElement implements MappingElement {
	private final PojoRawTypeIdentifier<?> declaringType;
	private final String declaringPropertyName;
	// Having multiple @IndexedEmbeddeds on the same property with the same prefix is forbidden,
	// so this will allow us to distinguish between the @IndexedEmbeddeds on the same property.
	private final String relativePrefix;

	public PojoIndexedEmbeddedMappingElement(PojoRawTypeIdentifier<?> declaringType, String declaringPropertyName,
			String relativePrefix) {
		this.declaringType = declaringType;
		this.declaringPropertyName = declaringPropertyName;
		this.relativePrefix = relativePrefix;
	}

	@Override
	public String toString() {
		if ( relativePrefix == null ) {
			return "@IndexedEmbedded(...)";
		}
		else if ( relativePrefix.indexOf( "." ) < ( relativePrefix.length() - 1 ) ) {
			return "@IndexedEmbedded(prefix = \"" + relativePrefix + "\", ...)";
		}
		else {
			return "@IndexedEmbedded(name = \"" + relativePrefix.substring( 0, relativePrefix.length() - 1 ) + "\", ...)";
		}
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		PojoIndexedEmbeddedMappingElement that = (PojoIndexedEmbeddedMappingElement) o;
		return Objects.equals( declaringType, that.declaringType )
				&& Objects.equals( declaringPropertyName, that.declaringPropertyName )
				&& Objects.equals( relativePrefix, that.relativePrefix );
	}

	@Override
	public int hashCode() {
		return Objects.hash( declaringType, declaringPropertyName, relativePrefix );
	}

	@Override
	public EventContext eventContext() {
		return PojoEventContexts.fromType( declaringType )
				.append( PojoEventContexts.fromPath( PojoModelPath.ofProperty( declaringPropertyName ) ) );
	}
}
