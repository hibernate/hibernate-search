/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping;


/**
 * A worker executing works as soon as they are submitted.
 * <p>
 * Relative ordering of works may not be preserved.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author Yoann Rodiere
 */
public interface StreamPojoWorker extends PojoWorker {

	void flush();

	void flush(Class<?> clazz);

	void optimize();

	void optimize(Class<?> clazz);

}
