/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;

public interface PojoCaster<T> {

	/**
	 * @param object the object to cast
	 * @return the cast object
	 * @throws RuntimeException If the object could not be cast
	 */
	T cast(Object object);

	/**
	 * @param object the object to cast
	 * @return the cast object, or {@code null} if the object could not be cast
	 */
	T castOrNull(Object object);

}
