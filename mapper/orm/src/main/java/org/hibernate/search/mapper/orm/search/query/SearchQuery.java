/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.query;

import java.util.List;
import java.util.Optional;

public interface SearchQuery<T> extends org.hibernate.search.engine.search.query.SearchQuery<T> {

	/**
	 * Execute the query and return the {@link SearchResult}.
	 *
	 * @return The {@link SearchResult}.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 * @throws org.hibernate.HibernateException If something goes wrong while fetching entities from the database.
	 * @throws javax.persistence.PersistenceException If something goes wrong while fetching entities from the database.
	 */
	@Override
	SearchResult<T> fetch();

	/**
	 * Execute the query and return the {@link SearchResult}.
	 *
	 * @param limit The maximum number of hits to be included in the {@link SearchResult}. {@code null} means no limit.
	 * @return The {@link SearchResult}.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 * @throws org.hibernate.HibernateException If something goes wrong while fetching entities from the database.
	 * @throws javax.persistence.PersistenceException If something goes wrong while fetching entities from the database.
	 */
	@Override
	SearchResult<T> fetch(Long limit);

	/**
	 * Execute the query and return the {@link SearchResult}.
	 *
	 * @param limit The maximum number of hits to be included in the {@link SearchResult}. {@code null} means no limit.
	 * @return The {@link SearchResult}.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 * @throws org.hibernate.HibernateException If something goes wrong while fetching entities from the database.
	 * @throws javax.persistence.PersistenceException If something goes wrong while fetching entities from the database.
	 */
	@Override
	SearchResult<T> fetch(Integer limit);

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
	@Override
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
	@Override
	SearchResult<T> fetch(Integer limit, Integer offset);

	/**
	 * Execute the query and return the hits as a {@link List}.
	 *
	 * @return The query hits.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 * @throws org.hibernate.HibernateException If something goes wrong while fetching entities from the database.
	 * @throws javax.persistence.PersistenceException If something goes wrong while fetching entities from the database.
	 */
	@Override
	List<T> fetchHits();

	/**
	 * Execute the query and return the hits as a {@link List}.
	 *
	 * @param limit The maximum number of hits to be returned by this method. {@code null} means no limit.
	 * @return The query hits.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 * @throws org.hibernate.HibernateException If something goes wrong while fetching entities from the database.
	 * @throws javax.persistence.PersistenceException If something goes wrong while fetching entities from the database.
	 */
	@Override
	List<T> fetchHits(Long limit);

	/**
	 * Execute the query and return the hits as a {@link List}.
	 *
	 * @param limit The maximum number of hits to be returned by this method. {@code null} means no limit.
	 * @return The query hits.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 * @throws org.hibernate.HibernateException If something goes wrong while fetching entities from the database.
	 * @throws javax.persistence.PersistenceException If something goes wrong while fetching entities from the database.
	 */
	@Override
	List<T> fetchHits(Integer limit);

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
	@Override
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
	@Override
	List<T> fetchHits(Integer limit, Integer offset);

	/**
	 * Execute the query and return the hits as a single, optional element.
	 *
	 * @return The single, optional query hit.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query,
	 * or the number of hits is more than one.
	 * @throws org.hibernate.HibernateException If something goes wrong while fetching entities from the database.
	 * @throws javax.persistence.PersistenceException If something goes wrong while fetching entities from the database.
	 */
	@Override
	Optional<T> fetchSingleHit();

	/**
	 * Execute the query and return the total hit count.
	 *
	 * @return The total number of matching entities, ignoring pagination settings.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 */
	@Override
	long fetchTotalHitCount();

}
