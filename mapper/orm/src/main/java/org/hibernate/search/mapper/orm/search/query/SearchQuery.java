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
	default SearchResult<T> fetch() {
		return fetch( (Long) null, null );
	}

	/**
	 * Execute the query and return the {@link SearchResult}.
	 *
	 * @param limit The maximum number of hits to be included in the {@link SearchResult}. {@code null} means no limit.
	 * @return The {@link SearchResult}.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 * @throws org.hibernate.HibernateException If something goes wrong while fetching entities from the database.
	 * @throws javax.persistence.PersistenceException If something goes wrong while fetching entities from the database.
	 */
	default SearchResult<T> fetch(Long limit) {
		return fetch( limit, null );
	}

	/**
	 * Execute the query and return the {@link SearchResult}.
	 *
	 * @param limit The maximum number of hits to be included in the {@link SearchResult}. {@code null} means no limit.
	 * @return The {@link SearchResult}.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 * @throws org.hibernate.HibernateException If something goes wrong while fetching entities from the database.
	 * @throws javax.persistence.PersistenceException If something goes wrong while fetching entities from the database.
	 */
	default SearchResult<T> fetch(Integer limit) {
		return fetch( limit == null ? null : limit.longValue(), null );
	}

	/**
	 * Execute the query and return the {@link SearchResult}.
	 *
	 * @param limit The maximum number of hits to be included in the {@link SearchResult}. {@code null} means no limit.
	 * @param offset The number of hits to skip before adding the hits to the {@link SearchResult}. {@code null} means no offset.
	 * @return The {@link SearchResult}.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 * @throws org.hibernate.HibernateException If something goes wrong while fetching entities from the database.
	 * @throws javax.persistence.PersistenceException If something goes wrong while fetching entities from the database.
	 */
	SearchResult<T> fetch(Long limit, Long offset);

	/**
	 * Execute the query and return the {@link SearchResult}.
	 *
	 * @param limit The maximum number of hits to be included in the {@link SearchResult}. {@code null} means no limit.
	 * @param offset The number of hits to skip before adding the hits to the {@link SearchResult}. {@code null} means no offset.
	 * @return The {@link SearchResult}.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 * @throws org.hibernate.HibernateException If something goes wrong while fetching entities from the database.
	 * @throws javax.persistence.PersistenceException If something goes wrong while fetching entities from the database.
	 */
	default SearchResult<T> fetch(Integer limit, Integer offset) {
		return fetch( limit == null ? null : (long) limit, offset == null ? null : (long) offset );
	}

	/**
	 * Execute the query and return the hits as a {@link List}.
	 *
	 * @return The query hits.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 * @throws org.hibernate.HibernateException If something goes wrong while fetching entities from the database.
	 * @throws javax.persistence.PersistenceException If something goes wrong while fetching entities from the database.
	 */
	default List<T> fetchHits() {
		return fetchHits( (Long) null, null );
	}

	/**
	 * Execute the query and return the hits as a {@link List}.
	 *
	 * @param limit The maximum number of hits to be returned by this method. {@code null} means no limit.
	 * @return The query hits.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 * @throws org.hibernate.HibernateException If something goes wrong while fetching entities from the database.
	 * @throws javax.persistence.PersistenceException If something goes wrong while fetching entities from the database.
	 */
	default List<T> fetchHits(Long limit) {
		return fetchHits( limit, null );
	}

	/**
	 * Execute the query and return the hits as a {@link List}.
	 *
	 * @param limit The maximum number of hits to be returned by this method. {@code null} means no limit.
	 * @return The query hits.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 * @throws org.hibernate.HibernateException If something goes wrong while fetching entities from the database.
	 * @throws javax.persistence.PersistenceException If something goes wrong while fetching entities from the database.
	 */
	default List<T> fetchHits(Integer limit) {
		return fetchHits( limit == null ? null : limit.longValue(), null );
	}

	/**
	 * Execute the query and return the hits as a {@link List}.
	 *
	 * @param limit The maximum number of hits to be returned by this method. {@code null} means no limit.
	 * @param offset The number of hits to skip. {@code null} means no offset.
	 * @return The query hits.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 * @throws org.hibernate.HibernateException If something goes wrong while fetching entities from the database.
	 * @throws javax.persistence.PersistenceException If something goes wrong while fetching entities from the database.
	 */
	List<T> fetchHits(Long limit, Long offset);

	/**
	 * Execute the query and return the hits as a {@link List}.
	 *
	 * @param limit The maximum number of hits to be returned by this method. {@code null} means no limit.
	 * @param offset The number of hits to skip. {@code null} means no offset.
	 * @return The query hits.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 * @throws org.hibernate.HibernateException If something goes wrong while fetching entities from the database.
	 * @throws javax.persistence.PersistenceException If something goes wrong while fetching entities from the database.
	 */
	default List<T> fetchHits(Integer limit, Integer offset) {
		return fetchHits( limit == null ? null : (long) limit, offset == null ? null : (long) offset );
	}

	/**
	 * Execute the query and return the hits as a single, optional element.
	 *
	 * @return The single, optional query hit.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query,
	 * or the number of hits is more than one.
	 * @throws org.hibernate.HibernateException If something goes wrong while fetching entities from the database.
	 * @throws javax.persistence.PersistenceException If something goes wrong while fetching entities from the database.
	 */
	Optional<T> fetchSingleHit();

	/**
	 * Execute the query and return the total hit count.
	 *
	 * @return The total number of matching entities, ignoring pagination settings.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 */
	long fetchTotalHitCount();

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
