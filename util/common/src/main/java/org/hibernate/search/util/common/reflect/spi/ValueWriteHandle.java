/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.reflect.spi;

/**
 * A handle giving write access to a property within an object instance:
 * field, setter method, ...
 */
public interface ValueWriteHandle {

	void set(Object thiz, Object value);

	/**
	 * @return {@code true} if {@code obj} is a {@link ValueWriteHandle} referencing the exact same
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
