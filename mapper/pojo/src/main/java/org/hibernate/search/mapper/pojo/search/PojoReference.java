/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.search;


/**
 * A reference to a POJO entity.
 */
public interface PojoReference {

	/**
	 * @return The type of the referenced entity.
	 */
	Class<?> getType();

	/**
	 * @return The identifier of the referenced entity.
	 */
	Object getId();

}
