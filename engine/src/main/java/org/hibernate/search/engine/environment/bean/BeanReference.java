/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean;

/**
 * @author Yoann Rodiere
 */
public interface BeanReference {

	/**
	 * @return The name of the referenced bean.
	 * {@code null} implies no name reference.
	 */
	String getName();

	/**
	 * @return The type of the referenced bean.
	 * {@code null} implies no type reference.
	 */
	Class<?> getType();

	/**
	 * Create a {@link BeanReference} referencing a bean by its name.
	 * <p>
	 * Note: when no dependency injection framework is used, Hibernate Search uses reflection to resolve beans,
	 * and in that case "names" are interpreted as fully qualified class names.
	 *
	 * @param name The bean name.
	 * @return The corresponding {@link BeanReference}.
	 */
	static BeanReference ofName(String name) {
		return new ImmutableBeanReference( name, null );
	}

	/**
	 * Create a {@link BeanReference} referencing a bean by its type.
	 *
	 * @param type The bean type.
	 * @return The corresponding {@link BeanReference}.
	 */
	static BeanReference ofType(Class<?> type) {
		return new ImmutableBeanReference( null, type );
	}

	/**
	 * Create a {@link BeanReference} referencing a bean by its name and type.
	 *
	 * @param name The bean name.
	 * @param type The bean type.
	 * @return The corresponding {@link BeanReference}.
	 */
	static BeanReference of(String name, Class<?> type) {
		return new ImmutableBeanReference( name, type );
	}

}
