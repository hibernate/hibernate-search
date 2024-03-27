/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.test.function;

@FunctionalInterface
public interface ThrowingConsumer<T, E extends Throwable> {
	void accept(T t) throws E;
}
