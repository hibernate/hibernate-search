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
	 * Get the bean this reference points to using the given provider.
	 *
	 * @param beanProvider A provider to get the bean from.
	 * @param expectedType The expected type of the bean.
	 * @param <T> The expected type of the bean.
	 * @return The bean instance.
	 */
	<T> T getBean(BeanProvider beanProvider, Class<T> expectedType);

	/**
	 * Create a {@link BeanReference} referencing a bean by its name.
	 * <p>
	 * Note: when no dependency injection framework is used, Hibernate Search uses reflection to resolve beans,
	 * and in that case "names" are interpreted as fully qualified class names.
	 *
	 * @param name The bean name. Must not be null nor empty.
	 * @return The corresponding {@link BeanReference}.
	 */
	static BeanReference ofName(String name) {
		return new NameBeanReference( name );
	}

	/**
	 * Create a {@link BeanReference} referencing a bean by its type.
	 *
	 * @param type The bean type. Must not be null.
	 * @return The corresponding {@link BeanReference}.
	 */
	static BeanReference ofType(Class<?> type) {
		return new TypeBeanReference( type );
	}

	/**
	 * Create a {@link BeanReference} referencing a bean by its name or type, or both.
	 *
	 * @param type The bean type. May be null, but only if {@code name} is not null.
	 * @param name The bean name. May be null, but only if {@code type} is not null.
	 * @return The corresponding {@link BeanReference}.
	 */
	static BeanReference of(Class<?> type, String name) {
		return TypeAndNameBeanReference.createLenient( type, name );
	}

}
