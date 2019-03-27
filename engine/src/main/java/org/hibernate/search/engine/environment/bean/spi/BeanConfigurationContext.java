/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean.spi;

public interface BeanConfigurationContext {

	/**
	 * Define a way to resolve a bean referenced by its {@code exposedType}.
	 * <p>
	 * Affects the behavior of {@link org.hibernate.search.engine.environment.bean.BeanProvider#getBean(Class)}
	 * in particular.
	 *
	 * @param exposedType The type that this definition will match (exact match: inheritance is ignored).
	 * @param factory The factory allowing to create the bean.
	 * @param <T> The exposed type of the bean.
	 */
	<T> void define(Class<T> exposedType, BeanFactory<T> factory);

	/**
	 * Define a way to resolve a bean referenced by its {@code exposedType} and {@code name}.
	 * <p>
	 * Affects the behavior of {@link org.hibernate.search.engine.environment.bean.BeanProvider#getBean(Class, String)}
	 * in particular.
	 *
	 * @param exposedType The type that this definition will match (exact match: inheritance is ignored).
	 * @param name The name that this definition will match (exact match: case is taken into account).
	 * @param factory The factory allowing to create the bean.
	 * @param <T> The exposed type of the bean.
	 */
	<T> void define(Class<T> exposedType, String name, BeanFactory<T> factory);

}
