/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.entity;

import java.io.Closeable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Hibernate-Search is no object storage. All hits found on the Index have a original representation. This interface
 * provides means to retrieve these when executing a {@link org.hibernate.search.genericjpa.query.HSearchQuery}
 *
 * @author Martin Braun
 */
public interface EntityProvider extends Closeable {

	default Object get(Class<?> entityClass, Object id) {
		return this.get( entityClass, id, Collections.emptyMap() );
	}

	Object get(Class<?> entityClass, Object id, Map<String, Object> hints);

	default List getBatch(Class<?> entityClass, List<Object> id) {
		return this.getBatch( entityClass, id, Collections.emptyMap() );
	}

	/**
	 * ATTENTION: ORDER IS NOT PRESERVED!
	 */
	@SuppressWarnings("rawtypes")
	List getBatch(Class<?> entityClass, List<Object> id, Map<String, Object> hints);

}
