/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.spi;

import org.hibernate.search.util.common.reflect.spi.ValueHandleFactory;

/**
 * A Pojo introspector used at bootstrap.
 */
public interface PojoBootstrapIntrospector {

	/**
	 * @param clazz The Java class representing the raw version of the type
	 * @param <T> The type
	 * @return A type model for the given type.
	 */
	<T> PojoRawTypeModel<T> typeModel(Class<T> clazz);

	/**
	 * @param name The name of the type
	 * @return A type model for the requested type.
	 */
	PojoRawTypeModel<?> typeModel(String name);

	/**
	 * @return A {@link ValueHandleFactory} for reading annotation attributes.
	 */
	ValueHandleFactory annotationValueHandleFactory();

	/**
	 * @return A {@link ValueHandleFactory} for reading annotation attributes.
	 * @deprecated Use/implement {@link #annotationValueHandleFactory()} instead.
	 */
	@Deprecated
	default org.hibernate.search.util.common.reflect.spi.ValueReadHandleFactory annotationValueReadHandleFactory() {
		return (org.hibernate.search.util.common.reflect.spi.ValueReadHandleFactory) annotationValueHandleFactory();
	}

}
