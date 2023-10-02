/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.reflect.spi;

/**
 * A handle giving read access to a value from an object instance:
 * field, no-argument method, ...
 *
 * @param <T> The value type.
 */
public interface ValueReadHandle<T> {

	T get(Object thiz);

	/**
	 * @return {@code true} if {@code obj} is a {@link ValueReadHandle} referencing the exact same
	 * value accessor: same API (java.lang.invoke or java.lang.reflect),
	 * same element (same field or method), ...
	 */
	@Override
	boolean equals(Object obj);

	/*
	 * Note to implementors: you must override hashCode to be consistent with equals().
	 */
	@Override
	int hashCode();

}
