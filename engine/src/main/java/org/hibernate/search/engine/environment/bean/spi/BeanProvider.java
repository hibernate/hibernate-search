/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean.spi;


import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.util.common.SearchException;

/**
 * The interface to be implemented by components providing beans to Hibernate Search.
 * <p>
 * This interface should only be called by the Hibernate Search engine itself;
 * if you are looking for retrieving beans from another module,
 * you should use {@link BeanResolver} instead.
 */
public interface BeanProvider extends AutoCloseable {

	/**
	 * Release any internal resource created while resolving beans.
	 * <p>
	 * Provided beans will not be usable after a call to this method.
	 * <p>
	 * May not release all resources that were allocated for each {@link BeanHolder};
	 * {@link BeanHolder#close()} still needs to be called consistently for each created bean.
	 *
	 * @see AutoCloseable#close()
	 */
	@Override
	void close();

	/**
	 * Resolve a bean by its type.
	 * @param <T> The expected return type.
	 * @param typeReference The type of the bean to resolve.
	 * @return A {@link BeanHolder} containing the resolved bean.
	 * @throws SearchException if the bean cannot be resolved.
	 */
	<T> BeanHolder<T> resolve(Class<T> typeReference);

	/**
	 * Resolve a bean by its name.
	 * @param <T> The expected return type.
	 * @param typeReference The type of the bean to resolve.
	 * @param nameReference The name of the bean to resolve.
	 * @return A {@link BeanHolder} containing the resolved bean.
	 * @throws SearchException if the bean cannot be resolved.
	 */
	<T> BeanHolder<T> resolve(Class<T> typeReference, String nameReference);

}
