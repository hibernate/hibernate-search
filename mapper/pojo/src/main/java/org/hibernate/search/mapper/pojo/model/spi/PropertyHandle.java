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

	Class<?> getType();

	Object get(Object thiz);

	/*
	 * Note to implementors: you must override equals to treat two references
	 * to the same property (with the same access mode, i.e. field or method)
	 * as equal.
	 */
	@Override
	boolean equals(Object obj);

	/*
	 * Note to implementors: you must override hash to return the same hash code
	 * for two references to the same property (with the same access mode, i.e. field or method).
	 */
	@Override
	int hashCode();

}
