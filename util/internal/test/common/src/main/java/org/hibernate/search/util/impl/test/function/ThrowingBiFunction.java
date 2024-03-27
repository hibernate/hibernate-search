/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.test.function;

@FunctionalInterface
public interface ThrowingBiFunction<T, U, R, E extends Throwable> {
	R apply(T t, U u) throws E;
}
