/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.db.util.impl;

import java.util.List;

/**
 * The Wrapper for Queries used in the async-backend.
 *
 * see {@link EntityManagerFactoryWrapper} for more info
 *
 * @author Martin Braun
 */
public interface QueryWrapper {

	void setMaxResults(long maxResults);

	void setFirstResult(long firstResult);

	List getResultList();

	Object getSingleResult();

	int executeUpdate();

}
