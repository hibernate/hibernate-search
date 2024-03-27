/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.spi;

@FunctionalInterface
public interface ClosingOperator<T, E extends Throwable> {

	void close(T objectToClose) throws E;

}
