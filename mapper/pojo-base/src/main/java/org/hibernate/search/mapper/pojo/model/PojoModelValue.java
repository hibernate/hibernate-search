/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model;

/**
 * A model element representing a value bound to a bridge.
 *
 * @see org.hibernate.search.mapper.pojo.bridge.ValueBridge
 * @hsearch.experimental This type is under active development.
 *    Usual compatibility policies do not apply: incompatible changes may be introduced in any future release.
 */
public interface PojoModelValue<T> extends PojoModelElement {

	/**
	 * @return The {@link Class} representing the raw type of this value.
	 */
	Class<?> getRawType();

}
