/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding;

import java.util.Optional;

import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.util.common.SearchException;

public interface BindingContext {

	/**
	 * @return A bean provider, allowing the retrieval of beans,
	 * including CDI/Spring DI beans when in the appropriate environment.
	 */
	BeanResolver beanResolver();

	/**
	 * @param name The name of the param
	 * @return Get a param defined for the binder by the given name
	 * @throws SearchException if it does not exist a param having such name
	 * @deprecated Use {@link #param(String, Class)} instead.
	 */
	@Deprecated
	default Object param(String name) {
		return param( name, Object.class );
	}

	/**
	 * @param name The name of the param
	 * @param paramType The type of the parameter.
	 * @param <T> The type of the parameter.
	 * @return Get a param defined for the binder by the given name
	 * @throws SearchException if it does not exist a param having such name
	 */
	<T> T param(String name, Class<T> paramType);

	/**
	 * @param name The name of the param
	 * @return Get an optional param defined for the binder by the given name,
	 * a param having such name may either exist or not.
	 * @deprecated Use {@link #paramOptional(String, Class)} instead.
	 */
	@Deprecated
	default Optional<Object> paramOptional(String name) {
		return paramOptional( name, Object.class );
	}

	/**
	 * @param name The name of the param
	 * @param paramType The type of the parameter.
	 * @param <T> The type of the parameter.
	 * @return Get an optional param defined for the binder by the given name,
	 * a param having such name may either exist or not.
	 */
	<T> Optional<T> paramOptional(String name, Class<T> paramType);

}
