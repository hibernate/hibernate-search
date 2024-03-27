/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.test.function;

@FunctionalInterface
public interface ThrowingFunction<T, R, E extends Throwable> {
	R apply(T t) throws E;
}
