/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jpa;

import org.hibernate.Criteria;

/**
 * Hibernate specific interface for backwards compatibility
 * @author Martin Braun
 */
//TODO return HibernateFullTextQuery rather than Query in useful chain methods
public interface HibernateFullTextQuery extends FullTextQuery {

	/**
	 * Defines the Database Query used to load the Lucene results.
	 * Useful to load a given object graph by refining the fetch modes
	 *
	 * No projection (criteria.setProjection() ) allowed, the root entity must be the only returned type
	 * No where restriction can be defined either.
	 *
	 * @param criteria a query defined using {@link Criteria}
	 * @return {@code this} for method chaining
	 */
	HibernateFullTextQuery setCriteriaQuery(Criteria criteria);

}
