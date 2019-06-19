/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;

/**
 * A handle to a property of a POJO, allowing to get the value of that property from a POJO instance.
 *
 * @param <T> The property type.
 */
public interface PropertyHandle<T> {

	T get(Object thiz);

	/**
	 * @return {@code true} if {@code obj} is a {@link PropertyHandle} referencing the exact same property
	 * with the exact same access mode (for instance direct field access or getter access), {@code false} otherwise.
	 */
	@Override
	boolean equals(Object obj);

	/*
	 * Note to implementors: you must override hashCode to be consistent with equals().
	 */
	@Override
	int hashCode();

}
