/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;


/**
 * An {@link BridgedElement} is a value that can be processed into
 * an index document, be it composite (an entity) or atomic
 * (a primitive value).
 * <p>
 * {@link BridgedElement}s only provide access to a set of previously
 * registered paths, accessed through a {@link BridgedElementReader}.
 *
 * @see BridgedElementModel
 *
 * @author Yoann Rodiere
 */
public interface BridgedElement {

}
