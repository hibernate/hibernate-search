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
	 * Execute the query and return the {@link SearchResult}.
	 *
	 * @return The {@link SearchResult}.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 * @throws org.hibernate.HibernateException If something goes wrong while fetching entities from the database.
	 * @throws javax.persistence.PersistenceException If something goes wrong while fetching entities from the database.
	 */
	SearchResult<T> fetch();

	/**
	 * Execute the query and return the hits as a {@link List}.
	 *
	 * @return The query hits.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 * @throws org.hibernate.HibernateException If something goes wrong while fetching entities from the database.
	 * @throws javax.persistence.PersistenceException If something goes wrong while fetching entities from the database.
	 */
	List<T> fetchHits();

	/**
	 * Execute the query and return the hits as a single, optional element.
	 *
	 * @return The single, optional query hit.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 * @throws org.hibernate.HibernateException If something goes wrong while fetching entities from the database,
	 * or the number of hits is more than one.
	 * @throws javax.persistence.PersistenceException If something goes wrong while fetching entities from the database,
	 * or the number of hits is more than one.
	 */
	Optional<T> fetchSingleHit();

	/**
	 * Execute the query and retrieve the total hit count,
	 * ignoring pagination settings ({@link #setMaxResults(Long)} and {@link #setFirstResult(Long)}).
	 *
	 * @return The total number of matching entities, ignoring pagination settings.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 */
	long fetchHitCount();

	/**
	 * Set the maximum number of hits returned by this query.
	 * <p>
	 * The default is no limit.
	 *
	 * @param maxResults The maximum number of hits to return. Must be positive or zero; {@code null} resets to the default value.
	 * @return {@code this} for method chaining.
	 */
	SearchQuery<T> setMaxResults(Long maxResults);

	/**
	 * @deprecated Pass a {@code long} value and use {@link #setMaxResults(Long)} instead.
	 * @param maxResults The maximum number of hits to return. Must be positive or zero.
	 * @return {@code this} for method chaining.
	 */
	@Deprecated
	SearchQuery<T> setMaxResults(int maxResults);

	/**
	 * Set the offset of the first hit returned by this query.
	 * <p>
	 * The default offset is {@code 0}.
	 *
	 * @param firstResult The offset of the first hit. Must be positive or zero; {@code null} resets to the default value.
	 * @return {@code this} for method chaining.
	 */
	SearchQuery<T> setFirstResult(Long firstResult);

	/**
	 * @deprecated Pass a {@code long} value and use {@link #setFirstResult(Long)} instead.
	 * @param firstResult The offset of the first hit. Must be positive or zero.
	 * @return {@code this} for method chaining.
	 */
	@Deprecated
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
	Query<T> toOrmQuery();

}
