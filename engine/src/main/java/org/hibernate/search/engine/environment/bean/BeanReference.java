/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean;

import org.hibernate.search.util.common.impl.StringHelper;

/**
 * A reference to a bean, allowing the retrieval of that bean
 * when {@link #getBean(BeanResolver) passed} a {@link BeanResolver}.
 *
 * @param <T> The type of the referenced bean.
 */
public interface BeanReference<T> {

	/**
	 * Get the bean this reference points to using the given provider.
	 *
	 * @param beanResolver A provider to get the bean from.
	 * @return The bean instance.
	 */
	BeanHolder<T> getBean(BeanResolver beanResolver);

	/**
	 * Cast this reference into a reference whose {@link #getBean(BeanResolver)} method is is guaranteed to
	 * either fail or return an instance of the given type.
	 *
	 * @param expectedType The expected bean type.
	 * @param <U> The expected bean type.
	 * @return A bean reference.
	 * @throws ClassCastException If this reference is certain to never return an instance of the given type.
	 */
	default <U> BeanReference<? extends U> asSubTypeOf(Class<U> expectedType) {
		return new CastingBeanReference<>( this, expectedType );
	}

	/**
	 * Create a {@link BeanReference} referencing a bean by its type only.
	 *
	 * @param type The bean type. Must not be null.
	 * @param <T> The bean type.
	 * @return The corresponding {@link BeanReference}.
	 */
	static <T> BeanReference<T> of(Class<T> type) {
		return new TypeBeanReference<>( type );
	}

	/**
	 * Create a {@link BeanReference} referencing a bean by type and name.
	 *
	 * @param type The bean type. Must not be null.
	 * @param name The bean name. May be null or empty.
	 * @param <T> The bean type.
	 * @return The corresponding {@link BeanReference}.
	 */
	static <T> BeanReference<T> of(Class<T> type, String name) {
		if ( StringHelper.isNotEmpty( name ) ) {
			return new TypeAndNameBeanReference<>( type, name );
		}
		else {
			return new TypeBeanReference<>( type );
		}
	}

	/**
	 * Create a {@link BeanReference} referencing a bean instance directly.
	 *
	 * @param instance The bean instance. Must not be null.
	 * @param <T> The bean type.
	 * @return The corresponding {@link BeanReference}.
	 */
	static <T> BeanReference<T> ofInstance(T instance) {
		return new InstanceBeanReference<>( instance );
	}

}
