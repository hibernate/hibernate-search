/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query;

import java.util.List;
import java.util.Optional;

import org.hibernate.search.util.common.SearchException;

/**
 * A search query, allowing to fetch search results.
 *
 * @param <H> The type of query hits.
 */
public interface SearchQuery<H> {

	/**
	 * Execute the query and return the {@link SearchResult}.
	 *
	 * @return The {@link SearchResult}.
	 * @throws SearchException If something goes wrong while executing the query.
	 * @throws RuntimeException If something goes wrong while loading entities. The exact type depends on the mapper,
	 * e.g. HibernateException/PersistenceException for the Hibernate ORM mapper.
	 */
	SearchResult<H> fetch();

	/**
	 * Execute the query and return the {@link SearchResult}.
	 *
	 * @param limit The maximum number of hits to be included in the {@link SearchResult}. {@code null} means no limit.
	 * @return The {@link SearchResult}.
	 * @throws SearchException If something goes wrong while executing the query.
	 * @throws RuntimeException If something goes wrong while loading entities. The exact type depends on the mapper,
	 * e.g. HibernateException/PersistenceException for the Hibernate ORM mapper.
	 */
	SearchResult<H> fetch(Integer limit);

	/**
	 * Execute the query and return the {@link SearchResult}.
	 *
	 * @param limit The maximum number of hits to be included in the {@link SearchResult}. {@code null} means no limit.
	 * @param offset The number of hits to skip before adding the hits to the {@link SearchResult}. {@code null} means no offset.
	 * @return The {@link SearchResult}.
	 * @throws SearchException If something goes wrong while executing the query.
	 * @throws RuntimeException If something goes wrong while loading entities. The exact type depends on the mapper,
	 * e.g. HibernateException/PersistenceException for the Hibernate ORM mapper.
	 */
	SearchResult<H> fetch(Integer limit, Integer offset);

	/**
	 * Execute the query and return the hits as a {@link List}.
	 *
	 * @return The query hits.
	 * @throws SearchException If something goes wrong while executing the query.
	 * @throws RuntimeException If something goes wrong while loading entities. The exact type depends on the mapper,
	 * e.g. HibernateException/PersistenceException for the Hibernate ORM mapper.
	 */
	List<H> fetchHits();

	/**
	 * Execute the query and return the hits as a {@link List}.
	 *
	 * @param limit The maximum number of hits to be returned by this method. {@code null} means no limit.
	 * @return The query hits.
	 * @throws SearchException If something goes wrong while executing the query.
	 * @throws RuntimeException If something goes wrong while loading entities. The exact type depends on the mapper,
	 * e.g. HibernateException/PersistenceException for the Hibernate ORM mapper.
	 */
	List<H> fetchHits(Integer limit);

	/**
	 * Execute the query and return the hits as a {@link List}.
	 *
	 * @param limit The maximum number of hits to be returned by this method. {@code null} means no limit.
	 * @param offset The number of hits to skip. {@code null} means no offset.
	 * @return The query hits.
	 * @throws SearchException If something goes wrong while executing the query.
	 * @throws RuntimeException If something goes wrong while loading entities. The exact type depends on the mapper,
	 * e.g. HibernateException/PersistenceException for the Hibernate ORM mapper.
	 */
	List<H> fetchHits(Integer limit, Integer offset);

	/**
	 * Execute the query and return the hits as a single, optional element.
	 *
	 * @return The single, optional query hit.
	 * @throws SearchException If something goes wrong while executing the query,
	 * or the number of hits is more than one.
	 * @throws RuntimeException If something goes wrong while loading entities. The exact type depends on the mapper,
	 * e.g. HibernateException/PersistenceException for the Hibernate ORM mapper.
	 */
	Optional<H> fetchSingleHit();

	/**
	 * Execute the query and return the total hit count.
	 *
	 * @return The total number of matching entities, ignoring pagination settings.
	 * @throws SearchException If something goes wrong while executing the query.
	 */
	long fetchTotalHitCount();

	/**
	 * @return A textual representation of the query.
	 */
	String getQueryString();

	/**
	 * Extend the current query with the given extension,
	 * resulting in an extended query offering more options or a more detailed result type.
	 *
	 * @param extension The extension to the predicate DSL.
	 * @param <Q> The type of queries provided by the extension.
	 * @return The extended query.
	 * @throws SearchException If the extension cannot be applied (wrong underlying backend, ...).
	 */
	<Q> Q extension(SearchQueryExtension<Q, H> extension);

}
