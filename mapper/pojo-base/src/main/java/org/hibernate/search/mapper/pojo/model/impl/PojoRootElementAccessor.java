/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.impl;

import org.hibernate.search.mapper.pojo.model.PojoElementAccessor;

/**
 * @param <T> The type of the root element.
 */
class PojoRootElementAccessor<T> implements PojoElementAccessor<T> {

	PojoRootElementAccessor() {
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	@SuppressWarnings("unchecked") // By construction, this accessor will only be passed PojoElement returning type T
	public T read(Object parentElement) {
		return (T) parentElement;
	}

}
