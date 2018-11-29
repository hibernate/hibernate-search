/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean.spi;


import org.hibernate.search.engine.environment.bean.BeanProvider;
import org.hibernate.search.util.SearchException;

/**
 * The interface to be implemented by components providing beans to Hibernate Search.
 * <p>
 * This interface should only be called by the Hibernate Search engine itself;
 * if you are looking for retrieving beans from another module,
 * you should use {@link BeanProvider} instead.
 */
public interface BeanResolver extends AutoCloseable {

	/**
	 * Release any internal resource created while resolving beans.
	 * <p>
	 * Provided beans will not be usable after a call to this method.
	 *
	 * @see AutoCloseable#close()
	 */
	@Override
	void close();

	/**
	 * Resolve a bean by its type.
	 * @param <T> The expected return type.
	 * @param expectedClass The expected class. The returned bean must implement this class.
	 * @param typeReference The type of the bean to resolve.
	 * Depending on the implementation, this could be a superclass or superinterface of the resolved bean.
	 * @return The resolved bean.
	 * @throws SearchException if the bean cannot be resolved.
	 */
	<T> T resolve(Class<T> expectedClass, Class<?> typeReference);

	/**
	 * Resolve a bean by its name.
	 * @param <T> The expected return type.
	 * @param expectedClass The expected class. The returned bean must implement this class.
	 * @param nameReference The name of the bean to resolve.
	 * @return The resolved bean.
	 * @throws SearchException if the bean cannot be resolved.
	 */
	<T> T resolve(Class<T> expectedClass, String nameReference);

	/**
	 * Resolve a bean by its name <em>and</em> type.
	 * @param <T> The expected return type.
	 * @param expectedClass The expected class. The returned bean must implement this class.
	 * @param nameReference The name of the bean to resolve.
	 * @param typeReference The type of the bean to resolve.
	 * Depending on the implementation, this could be a superclass or superinterface of the resolved bean.
	 * @return The resolved bean.
	 * @throws SearchException if the bean cannot be resolved.
	 */
	<T> T resolve(Class<T> expectedClass, String nameReference, Class<?> typeReference);

}
