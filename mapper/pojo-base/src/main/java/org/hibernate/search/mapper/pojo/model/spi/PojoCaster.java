/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.spi;

public interface PojoCaster<T> {

	/**
	 * @param object the object to cast
	 * @return the cast object
	 * @throws RuntimeException If the object could not be cast
	 */
	T cast(Object object);

	/**
	 * @param object the object to cast
	 * @return the cast object, or {@code null} if the object could not be cast
	 */
	T castOrNull(Object object);

}
