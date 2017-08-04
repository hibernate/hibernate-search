/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.model.spi;


/**
 * An {@link Indexable} is a value that can be processed into
 * an index document, be it composite (an entity) or atomic
 * (a primitive value).
 * <p>
 * {@link Indexable}s only provide access to a set of previously
 * registered paths, referenced by their {@link IndexableReference}.
 *
 * @see IndexableModel
 *
 * @author Yoann Rodiere
 */
public interface Indexable {

	<T> T get(IndexableReference<T> key);

}
