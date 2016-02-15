/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jpa;

import java.util.List;
import java.util.Map;

/**
 * Created by Martin on 27.11.2015.
 */
public interface EntityProvider {

	Object get(Class<?> entityClass, Object id, Map<String, Object> hints);

	/**
	 * order is not important here
	 */
	@SuppressWarnings("rawtypes")
	List getBatch(Class<?> entityClass, List<Object> id, Map<String, Object> hints);
}
