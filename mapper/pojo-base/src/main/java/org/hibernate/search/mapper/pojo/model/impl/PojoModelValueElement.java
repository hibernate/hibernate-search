/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.impl;

import org.hibernate.search.mapper.pojo.model.PojoModelValue;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

/**
 * @param <T> The type used as a root element.
 */
public class PojoModelValueElement<T> implements PojoModelValue<T> {

	private final PojoBootstrapIntrospector introspector;
	private final PojoTypeModel<? extends T> typeModel;

	public PojoModelValueElement(PojoBootstrapIntrospector introspector,
			PojoTypeModel<? extends T> typeModel) {
		this.introspector = introspector;
		this.typeModel = typeModel;
	}

	@Override
	public String toString() {
		return "PojoModelValueElement[" + typeModel.toString() + "]";
	}

	@Override
	public boolean isAssignableTo(Class<?> clazz) {
		return typeModel.rawType().isSubTypeOf( introspector.typeModel( clazz ) );
	}

	@Override
	public Class<?> rawType() {
		return typeModel.rawType().typeIdentifier().javaClass();
	}
}
