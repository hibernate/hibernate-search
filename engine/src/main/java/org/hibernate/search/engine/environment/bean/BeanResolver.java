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
 * The main entry point for components looking to resolve a bean reference into a (usually user-provided) bean.
 * <p>
 * Depending on the integration, beans may be instantiated using reflection (expecting a no-argument constructor),
 * or provided by a more advanced dependency injection context (CDI, Spring DI).
 * <p>
 * Regardless of the underlying implementation, this interface is used to resolve beans,
 * referenced either
 * {@link #resolve(Class) by their type},
 * or {@link #resolve(Class, String) by their type and name},
 * or {@link #resolveRole(Class) by their role}.
 * <p>
 * This interface is API,
 * but should only be implemented by Hibernate Search itself;
 * if you are looking to provide beans from a different source,
 * you should implement {@link BeanProvider} instead.
 */
public interface BeanResolver {

	/**
	 * Resolve a bean by its type.
	 * @param <T> The expected return type.
	 * @param typeReference The type used as a reference to the bean to resolve. Must be non-null.
	 * @return A {@link BeanHolder} containing the resolved bean.
	 * @throws SearchException if the reference is invalid (null) or the bean cannot be resolved.
	 */
	<T> BeanHolder<T> resolve(Class<T> typeReference);

	/**
	 * Resolve a bean by its name.
	 * @param <T> The expected return type.
	 * @param typeReference The type used as a reference to the bean to resolve. Must be non-null.
	 * @param nameReference The name used as a reference to the bean to resolve. Must be non-null and non-empty.
	 * @return A {@link BeanHolder} containing the resolved bean.
	 * @throws SearchException if a reference is invalid (null or empty) or the bean cannot be resolved.
	 */
	<T> BeanHolder<T> resolve(Class<T> typeReference, String nameReference);

	/**
	 * Resolve a {@link BeanReference}.
	 * <p>
	 * This method is just syntactic sugar to allow writing {@code bridgeProvider::resolve}
	 * and getting a {@code Function<BeanReference<T>, T>} that can be used in {@link java.util.Optional#map(Function)}
	 * for instance.
	 *
	 * @param <T> The expected return type.
	 * @param reference The reference to the bean to resolve. Must be non-null.
	 * @return A {@link BeanHolder} containing the resolved bean.
	 * @throws SearchException if the reference is invalid (null or empty) or the bean cannot be resolved.
	 */
	default <T> BeanHolder<T> resolve(BeanReference<T> reference) {
		Contracts.assertNotNull( reference, "reference" );
		return reference.resolve( this );
	}

	/**
	 * Resolve a list of {@link BeanReference}s.
	 * <p>
	 * The main advantage of calling this method over looping and calling {@link #resolve(BeanReference)} repeatedly
	 * is that errors are handled correctly: if a bean was already instantiated, and getting the next one fails,
	 * then the first bean will be properly {@link BeanHolder#close() closed} before the exception is propagated.
	 * Also, this method returns a {@code BeanHolder<List<T>>} instead of a {@code List<BeanHolder<T>>},
	 * so its result is easier to use in a try-with-resources.
	 * <p>
	 * This method is also syntactic sugar to allow writing {@code bridgeProvider::resolve}
	 * and getting a {@code Function<BeanReference<T>, T>} that can be used in {@link java.util.Optional#map(Function)}
	 * for instance.
	 *
	 * @param <T> The expected bean type.
	 * @param references The references to the beans to retrieve. Must be non-null.
	 * @return A {@link BeanHolder} containing a {@link List} containing the resolved beans,
	 * in the same order as the {@code references}.
	 * @throws SearchException if one reference is invalid (null or empty) or the corresponding bean cannot be resolved.
	 */
	default <T> BeanHolder<List<T>> resolve(List<? extends BeanReference<? extends T>> references) {
		List<BeanHolder<? extends T>> beanHolders = new ArrayList<>();
		try {
			for ( BeanReference<? extends T> reference : references ) {
				beanHolders.add( reference.resolve( this ) );
			}
			return BeanHolder.of( beanHolders );
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e ).pushAll( BeanHolder::close, beanHolders );
			throw e;
		}
	}

	/**
	 * Resolve the given role into a list of beans.
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
	<T> BeanHolder<List<T>> resolveRole(Class<T> role);

}
