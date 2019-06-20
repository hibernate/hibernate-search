/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean;


import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.environment.bean.spi.BeanProvider;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.impl.SuppressingCloser;

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
 * you should implement {@link BeanProvider} instead.
 */
public interface BeanResolver {

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

	/**
	 * Retrieve a list of beans from a list of {@link BeanReference}s.
	 * <p>
	 * The main advantage of calling this method over looping and calling {@link #getBean(BeanReference)} repeatedly
	 * is that errors are handled correctly: if a bean was already instantiated, and getting the next one fails,
	 * then the first bean will be properly {@link BeanHolder#close() closed} before the exception is propagated.
	 * Also, this method returns a {@code BeanHolder<List<T>>} instead of a {@code List<BeanHolder<T>>},
	 * so its result is easier to use in a try-with-resources.
	 * <p>
	 * This method is also syntactic sugar to allow to write {@code bridgeProvider::getBeans}
	 * and get a {@code Function<BeanReference<T>, T>} that can be used in {@link java.util.Optional#map(Function)}
	 * for instance.
	 *
	 * @param <T> The expected bean type.
	 * @param references The references to the beans to retrieve. Must be non-null.
	 * @return A {@link BeanHolder} containing a {@link List} containing the resolved beans,
	 * in the same order as the {@code references}.
	 * @throws SearchException if one reference is invalid (null or empty) or the corresponding bean cannot be resolved.
	 */
	default <T> BeanHolder<List<T>> getBeans(List<? extends BeanReference<? extends T>> references) {
		List<BeanHolder<? extends T>> beanHolders = new ArrayList<>();
		try {
			for ( BeanReference<? extends T> reference : references ) {
				beanHolders.add( reference.getBean( this ) );
			}
			return BeanHolder.of( beanHolders );
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e ).pushAll( BeanHolder::close, beanHolders );
			throw e;
		}
	}

	/**
	 * Retrieve a list of beans with the given role.
	 * <p>
	 * <strong>WARNING:</strong> this does not just return all the beans that implement {@code role}.
	 * Beans are assigned a role explicitly during
	 * {@link org.hibernate.search.engine.environment.bean.spi.BeanConfigurer bean configuration}
	 * by calling
	 * {@link org.hibernate.search.engine.environment.bean.spi.BeanConfigurationContext#assignRole(Class, BeanReference)}.
	 *
	 * @param <T> The expected bean type.
	 * @param role The role that must have been assigned to the retrieved beans. Must be non-null and non-empty.
	 * @return A {@link BeanHolder} containing a {@link List} containing the resolved beans.
	 * @throws SearchException if one of the references assigned to the role cannot be resolved.
	 */
	<T> BeanHolder<List<T>> getBeansWithRole(Class<T> role);

}
