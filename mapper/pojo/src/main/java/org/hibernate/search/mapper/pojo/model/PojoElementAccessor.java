/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model;


/**
 * An accessor allowing to retrieve an element of a POJO, e.g. a property.
 * <p>
 * Accessors are created by {@link PojoModelCompositeElement} instances.
 * @hsearch.experimental This type is under active development.
 *    You should be prepared for incompatible changes in future releases.
 */
public interface PojoElementAccessor<T> {

	T read(Object bridgedElement);

}
