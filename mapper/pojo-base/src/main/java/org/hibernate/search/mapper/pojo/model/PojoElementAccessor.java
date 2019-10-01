/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model;


/**
 * An accessor allowing the retrieval of an element, for example a property, from a POJO.
 * <p>
 * Accessors are created by {@link PojoModelCompositeElement} instances.
 * @hsearch.experimental This type is under active development.
 *    Usual compatibility policies do not apply: incompatible changes may be introduced in any future release.
 */
public interface PojoElementAccessor<T> {

	/**
	 * Reads the element from the given parent element.
	 * @param parentElement An object compatible with this accessor, i.e. with the right type.
	 * @return The element pointed to by this accessor, extracted from the given parent element.
	 */
	T read(Object parentElement);

}
