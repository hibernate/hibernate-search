/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean.spi;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;

public interface BeanConfigurationContext {

	/**
	 * Define a way to resolve a bean referenced by its {@code exposedType}.
	 * <p>
	 * Affects the behavior of {@link BeanResolver#resolve(Class)}
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
	 * Affects the behavior of {@link BeanResolver#resolve(Class, String)}
	 * in particular.
	 *
	 * @param exposedType The type that this definition will match (exact match: inheritance is ignored).
	 * @param name The name that this definition will match (exact match: case is taken into account).
	 * @param factory The factory allowing to create the bean.
	 * @param <T> The exposed type of the bean.
	 */
	<T> void define(Class<T> exposedType, String name, BeanFactory<T> factory);

	/**
	 * Assign a role to a bean reference.
	 * <p>
	 * Affects the behavior of {@link BeanResolver#resolveRole(Class)}
	 * in particular.
	 * <p>
	 * Roles allow to overcome limitations of the Spring/CDI integrations,
	 * which can only ever return one bean for a given reference
	 * (obviously that's a limitation of our SPI, not of Spring/CDI).
	 * With roles, you can define multiple beans in Spring/CDI, and assign the same role to all of them.
	 * to separate the bean definition from the selection of the beans to use in Hibernate Search:
	 * you can define a hundred beans implementing a given interface,
	 * but only assign a role to one of them, based on configuration.
	 *
	 * @param role A type representing the role that this reference will match (exact match: inheritance is ignored).
	 * @param reference A reference to a bean to use when the given role is requested.
	 * @param <T> The role type.
	 */
	<T> void assignRole(Class<T> role, BeanReference<? extends T> reference);

}
