/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;

/**
 * @author Yoann Rodiere
 */
public interface PropertyHandle {

	String getName();

	Object get(Object thiz);

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
