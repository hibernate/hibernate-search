/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.environment.bean.spi;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.util.common.SearchException;

/**
 * The interface to be implemented by components providing beans to Hibernate Search.
 * <p>
 * This interface should only be called by Hibernate Search itself;
 * if you are looking to retrieve beans,
 * you should use {@link BeanResolver} instead.
 */
public interface BeanProvider extends AutoCloseable {

	/**
	 * Release any internal resource created to support provided beans.
	 * <p>
	 * Provided beans will not be usable after a call to this method.
	 * <p>
	 * This may not release all resources that were allocated for each {@link BeanHolder};
	 * {@link BeanHolder#close()} still needs to be called consistently for each created bean.
	 *
	 * @see AutoCloseable#close()
	 */
	@Override
	void close();

	/**
	 * Provide a bean referenced by its type.
	 * @param <T> The expected return type.
	 * @param typeReference The type used as a reference to the bean to retrieve. Must be non-null.
	 * @return A {@link BeanHolder} containing the resolved bean.
	 * @throws BeanNotFoundException if the bean does not exist.
	 * @throws SearchException if the reference is invalid (null) or an unexpected error occurs.
	 */
	<T> BeanHolder<T> forType(Class<T> typeReference);

	/**
	 * Provide a bean referenced by its type and name.
	 * @param <T> The expected return type.
	 * @param typeReference The type used as a reference to the bean to retrieve. Must be non-null.
	 * @param nameReference The name used as a reference to the bean to retrieve. Must be non-null and non-empty.
	 * @return A {@link BeanHolder} containing the resolved bean.
	 * @throws BeanNotFoundException if the bean does not exist.
	 * @throws SearchException if a reference is invalid (null or empty) or an unexpected error occurs.
	 */
	<T> BeanHolder<T> forTypeAndName(Class<T> typeReference, String nameReference);

}
