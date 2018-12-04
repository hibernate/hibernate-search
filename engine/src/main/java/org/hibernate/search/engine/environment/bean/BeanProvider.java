/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean;


import java.util.function.Function;

import org.hibernate.search.engine.environment.bean.spi.BeanResolver;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.common.Contracts;

/**
 * The main entry point for components looking to retrieve user-provided beans.
 * <p>
 * Depending on the integration, beans may be resolved using reflection (expecting a no-argument constructor),
 * or using a more advanced dependency injection context (CDI, Spring DI).
 * <p>
 * Regardless of the implementations, this interface is used to retrieve the beans,
 * referenced either by their name, by their type, or both.
 * <p>
 * This interface may be used by any Hibernate Search module,
 * but should only be implemented by the Hibernate Search engine itself;
 * if you are looking for implementing your own bean resolver,
 * you should implement {@link BeanResolver} instead.
 */
public interface BeanProvider {

	/**
	 * Retrieve a bean referenced by its type.
	 * @param <T> The expected return type.
	 * @param typeReference The type used as a reference to the bean to retrieve. Must be non-null.
	 * @return A {@link BeanHolder} containing the resolved bean.
	 * @throws SearchException if the reference is invalid (null) or the bean cannot be resolved.
	 */
	<T> BeanHolder<T> getBean(Class<T> typeReference);

	/**
	 * Retrieve a bean referenced by its type and name.
	 * @param <T> The expected return type.
	 * @param typeReference The type used as a reference to the bean to retrieve. Must be non-null.
	 * @param nameReference The name used as a reference to the bean to retrieve. Must be non-null and non-empty.
	 * @return A {@link BeanHolder} containing the resolved bean.
	 * @throws SearchException if the reference is invalid (null or empty) or the bean cannot be resolved.
	 */
	<T> BeanHolder<T> getBean(Class<T> typeReference, String nameReference);

	/**
	 * Retrieve a bean from a {@link BeanReference}.
	 * <p>
	 * This method is just syntactic sugar to allow to write {@code bridgeProvider::getBean}
	 * and get a {@code Function<BeanReference<T>, T>} that can be used in {@link java.util.Optional#map(Function)}
	 * for instance.
	 *
	 * @param <T> The expected return type.
	 * @param reference The reference to the bean to retrieve. Must be non-null.
	 * @return A {@link BeanHolder} containing the resolved bean.
	 * @throws SearchException if the reference is invalid (null or empty) or the bean cannot be resolved.
	 */
	default <T> BeanHolder<T> getBean(BeanReference<T> reference) {
		Contracts.assertNotNull( reference, "reference" );
		return reference.getBean( this );
	}

}
