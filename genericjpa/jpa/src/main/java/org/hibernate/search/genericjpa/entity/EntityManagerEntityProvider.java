/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.entity;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;

/**
 * if you are using the EntityManager passed to this interface, make sure to detach your entities!
 *
 * @hsearch.experimental
 */
public interface EntityManagerEntityProvider {

	Object get(EntityManager em, Class<?> entityClass, Object id, Map<String, Object> hints);

	List getBatch(EntityManager em, Class<?> entityClass, List<Object> id, Map<String, Object> hints);

}
