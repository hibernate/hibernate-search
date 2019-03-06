/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.query;

import java.util.List;
import java.util.Optional;
import javax.persistence.TypedQuery;

import org.hibernate.query.Query;

public interface SearchQuery<T> {

	/**
	 * Execute the query and retrieve the results as a {@link List}
	 *
	 * @return The results as a {@link List} containing one element for each matched entity.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 * @throws org.hibernate.HibernateException If something goes wrong while fetching results from the database.
	 * @throws javax.persistence.PersistenceException If something goes wrong while fetching results from the database.
	 */
	List<T> getResultList();

	/**
	 * Execute the query and retrieve the results as a single element.
	 *
	 * @return The result.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 * @throws org.hibernate.HibernateException If something goes wrong while fetching results from the database,
	 * or the number of results is not exactly one.
	 * @throws javax.persistence.PersistenceException If something goes wrong while fetching results from the database,
	 * or the number of results is not exactly one.
	 */
	T getSingleResult();

	/**
	 * Execute the query and retrieve the results as a single, optional element.
	 *
	 * @return The result.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 * @throws org.hibernate.HibernateException If something goes wrong while fetching results from the database,
	 * or the number of results is more than one.
	 * @throws javax.persistence.PersistenceException If something goes wrong while fetching results from the database,
	 * or the number of results is more than one.
	 */
	Optional<T> getOptionalResult();

	/**
	 * Execute the query and retrieve the total result size,
	 * ignoring pagination settings ({@link #setMaxResults(int)} and {@link #setFirstResult(int)}).
	 *
	 * @return The total number of results, ignoring pagination settings.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 */
	long getResultSize();

	/**
	 * Set the maximum number of results returned by this query.
	 * <p>
	 * The default is no limit.
	 *
	 * @param maxResults The maximum number of results to return. Must be positive or zero.
	 * @return {@code this} for method chaining.
	 */
	SearchQuery<T> setMaxResults(int maxResults);

	/**
	 * Set the offset of the first result returned by this query.
	 * <p>
	 * The default offset is {@code 0}.
	 *
	 * @param firstResult The offset of the first result. Must be positive or zero.
	 * @return {@code this} for method chaining.
	 */
	SearchQuery<T> setFirstResult(int firstResult);

	/**
	 * Set the JDBC fetch size for this query.
	 *
	 * @param fetchSize The fetch size. Must be positive or zero.
	 * @return {@code this} for method chaining.
	 * @see Query#setFetchSize(int)
	 */
	SearchQuery<T> setFetchSize(int fetchSize);

	/**
	 * Convert this query to a {@link TypedQuery JPA query}.
	 * <p>
	 * Note that the resulting query <strong>does not support all operations</strong>
	 * and may behave slightly differently in some cases
	 * (including, but not limited to, the type of thrown exceptions).
	 * For these reasons, it is recommended to only use this method
	 * when integrating to an external library that expects JPA queries.
	 *
	 * @return A representation of this query as a JPA query.
	 */
	TypedQuery<T> toJpaQuery();

	/**
	 * Convert this query to a {@link Query Hibernate ORM query}.
	 * <p>
	 * Note that the resulting query <strong>does not support all operations</strong>
	 * and may behave slightly differently in some cases
	 * (including, but not limited to, the type of thrown exceptions).
	 * For these reasons, it is recommended to only use this method
	 * when integrating to an external library that expects Hibernate ORM queries.
	 *
	 * @return A representation of this query as a Hibernate ORM query.
	 */
	Query<T> toHibernateOrmQuery();

}
