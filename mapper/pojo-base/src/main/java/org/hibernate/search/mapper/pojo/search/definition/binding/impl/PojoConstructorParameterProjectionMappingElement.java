/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.search.definition.binding.impl;

import java.util.Objects;

import org.hibernate.search.engine.mapper.model.spi.MappingElement;
import org.hibernate.search.mapper.pojo.model.spi.PojoConstructorModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoMethodParameterModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.reporting.spi.PojoEventContexts;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBinder;
import org.hibernate.search.util.common.reporting.EventContext;

final class PojoConstructorParameterProjectionMappingElement implements MappingElement {
	private final PojoRawTypeIdentifier<?> declaringProjectionConstructorType;
	private final PojoMethodParameterModel<?> declaringParameter;
	private final ProjectionBinder binder;

	public PojoConstructorParameterProjectionMappingElement(PojoConstructorModel<?> declaringProjectionConstructor,
			PojoMethodParameterModel<?> declaringParameter,
			ProjectionBinder binder) {
		// We don't need to distinguish between constructors,
		// since there can only ever be one projection constructor per type.
		// This helps with error messages as displaying a constructor is very verbose.
		this.declaringProjectionConstructorType = declaringProjectionConstructor.typeModel().typeIdentifier();
		this.declaringParameter = declaringParameter;
		this.binder = binder;
	}

	@Override
	public String toString() {
		return binder.toString();
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		PojoConstructorParameterProjectionMappingElement that = (PojoConstructorParameterProjectionMappingElement) o;
		return Objects.equals( declaringProjectionConstructorType, that.declaringProjectionConstructorType )
				&& Objects.equals( declaringParameter, that.declaringParameter );
	}

	@Override
	public int hashCode() {
		return Objects.hash( declaringProjectionConstructorType, declaringParameter );
	}

	@Override
	public EventContext eventContext() {
		return PojoEventContexts.fromType( declaringProjectionConstructorType )
				.append( PojoEventContexts.projectionConstructor() )
				.append( PojoEventContexts.fromMethodParameter( declaringParameter ) );
	}
}
