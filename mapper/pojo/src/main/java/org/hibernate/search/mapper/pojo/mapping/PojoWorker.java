/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping;

/**
 * A entry point to execute works on POJO-mapped indexes.
 * <p>
 * Implementations may not be thread-safe.
 *
 * @author Yoann Rodiere
 */
public interface PojoWorker {

	void add(Object entity);

	void add(Object id, Object entity);

	void update(Object entity);

	void update(Object id, Object entity);

	void delete(Class<?> clazz, Object id);

}
