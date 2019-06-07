/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model;

/**
 * An element in the POJO model.
 *
 * @see PojoModelType
 * @see PojoModelProperty
 * @hsearch.experimental This type is under active development.
 *    Usual compatibility policies do not apply: incompatible changes may be introduced in any future release.
 */
public interface PojoModelElement {

	/**
	 * @param clazz A {@link Class}.
	 * @return {@code true} if instances of the given class can be assigned to this element,
	 * {@code false} otherwise.
	 */
	boolean isAssignableTo(Class<?> clazz);

}
