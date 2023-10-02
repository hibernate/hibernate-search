/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.reflect.spi;

import java.util.function.Function;

/**
 * A handle enabling the creation of a value from an array of arguments:
 * Java class constructor, ...
 *
 * @param <T> The value type.
 */
public interface ValueCreateHandle<T> extends Function<Object[], T> {

	T create(Object... arguments);

	@Override
	default T apply(Object[] objects) {
		return create( objects );
	}

	/**
	 * @return {@code true} if {@code obj} is a {@link ValueCreateHandle} referencing the exact same
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
