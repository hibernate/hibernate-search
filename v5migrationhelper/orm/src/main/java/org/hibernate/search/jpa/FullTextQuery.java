/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.jpa;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.Query;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchQueryExtension;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.mapper.orm.search.loading.dsl.SearchLoadingOptionsStep;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.query.DatabaseRetrievalMethod;
import org.hibernate.search.query.ObjectLookupMethod;
import org.hibernate.search.query.engine.spi.FacetManager;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.transform.ResultTransformer;

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Sort;

/**
 * The base interface for full-text queries using the JPA API ({@link jakarta.persistence.Query}).
 *
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@link SearchSession}
 * using {@link org.hibernate.search.mapper.orm.Search#session(EntityManager)},
 * then create a {@link SearchQuery} with {@link SearchSession#search(Class)}.
 * If you really need an adapter to JPA's {@link Query},
 * convert that {@link SearchQuery} using {@link org.hibernate.search.mapper.orm.Search#toJpaQuery(SearchQuery)},
 * but be aware that only part of the contract is implemented.
 * Refer to the <a href="https://hibernate.org/search/documentation/migrate/6.0/">migration guide</a> for more information.
 */
@Deprecated
public interface FullTextQuery extends Query, ProjectionConstants {

	/**
	 * Allows to let lucene sort the results. This is useful when you have
	 * additional sort requirements on top of the default lucene ranking.
	 * Without lucene sorting you would have to retrieve the full result set and
	 * order the hibernate objects.
	 *
	 * @param sort The lucene sort object.
	 *
	 * @return this for method chaining
	 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@link SearchSession}
	 * using {@link org.hibernate.search.mapper.orm.Search#session(EntityManager)},
	 * create a search query with {@link SearchSession#search(Class)},
	 * and define your sorts using {@link SearchQueryOptionsStep#sort(Function)}.
	 * Refer to the <a href="https://hibernate.org/search/documentation/migrate/6.0/">migration guide</a> for more information.
	 */
	@Deprecated
	FullTextQuery setSort(Sort sort);

	/**
	 * Returns the number of hits for this search
	 *
	 * Caution:
	 * The number of results might be slightly different from
	 * <code>getResultList().size()</code> because getResultList()
	 * may be not in sync with the database at the time of query.
	 *
	 * @return the number of hits for this search
	 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@link SearchSession}
	 * using {@link org.hibernate.search.mapper.orm.Search#session(EntityManager)},
	 * create a search query with {@link SearchSession#search(Class)},
	 * and get the total hit count ("result size") using {@link SearchQuery#fetchTotalHitCount()}.
	 * Refer to the <a href="https://hibernate.org/search/documentation/migrate/6.0/">migration guide</a> for more information.
	 */
	@Deprecated
	int getResultSize();

	/**
	 * Defines the Lucene field names projected and returned in a query result
	 * Each field is converted back to it's object representation, an Object[] being returned for each "row"
	 * (similar to an HQL or a Criteria API projection).
	 *
	 * A projectable field must be stored in the Lucene index and use a two-way field bridge
	 * Unless notified in their JavaDoc, all built-in bridges are two-way. All @DocumentId fields are projectable by design.
	 *
	 * If the projected field is not a projectable field, null is returned in the object[]
	 *
	 * @param fields the fields to use for projection
	 * @return {@code this} for method chaining
	 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@link SearchSession}
	 * using {@link org.hibernate.search.mapper.orm.Search#session(EntityManager)},
	 * create a search query with {@link SearchSession#search(Class)},
	 * and define your projections using {@link SearchQuerySelectStep#select(Function)}.
	 * Refer to the <a href="https://hibernate.org/search/documentation/migrate/6.0/">migration guide</a> for more information.
	 */
	@Deprecated
	FullTextQuery setProjection(String... fields);

	/**
	 * Defines the center of the spatial search for this query to project distance in results
	 *
	 * @param latitude latitude of the search center
	 * @param longitude longitude of the search center
	 * @param fieldName name of the spatial field
	 *
	 * @return {@code this} for method chaining
	 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@link SearchSession}
	 * using {@link org.hibernate.search.mapper.orm.Search#session(EntityManager)},
	 * create a search query with {@link SearchSession#search(Class)},
	 * and define your projections using {@link SearchQuerySelectStep#select(Function)}.
	 * See in particular the distance projection: {@link SearchProjectionFactory#distance(String, GeoPoint)}.
	 * Refer to the <a href="https://hibernate.org/search/documentation/migrate/6.0/">migration guide</a> for more information.
	 */
	@Deprecated
	FullTextQuery setSpatialParameters(double latitude, double longitude, String fieldName);

	/**
	 * Defines the center of the spatial search for this query to project distance in results
	 *
	 * @param center the search center
	 * @param fieldName name of the spatial field
	 *
	 * @return {@code this} for method chaining
	 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@link SearchSession}
	 * using {@link org.hibernate.search.mapper.orm.Search#session(EntityManager)},
	 * create a search query with {@link SearchSession#search(Class)},
	 * and define your projections using {@link SearchQuerySelectStep#select(Function)}.
	 * See in particular the distance projection: {@link SearchProjectionFactory#distance(String, GeoPoint)}.
	 * Refer to the <a href="https://hibernate.org/search/documentation/migrate/6.0/">migration guide</a> for more information.
	 */
	@Deprecated
	FullTextQuery setSpatialParameters(Coordinates center, String fieldName);

	/**
	 * @return return the manager for all faceting related operations
	 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@link SearchSession}
	 * using {@link org.hibernate.search.mapper.orm.Search#session(EntityManager)},
	 * create a search query with {@link SearchSession#search(Class)},
	 * and define your facets (now called aggregations)
	 * using {@link SearchQueryOptionsStep#aggregation(AggregationKey, Function)}.
	 * You can then fetch the query result using {@link SearchQuery#fetch(Integer)}
	 * and get each aggregation using {@link SearchResult#aggregation(AggregationKey)}.
	 * Refer to the <a href="https://hibernate.org/search/documentation/migrate/6.0/">migration guide</a> for more information.
	 */
	@Deprecated
	FacetManager getFacetManager();

	/**
	 * Defines a result transformer used during projection
	 *
	 * @param transformer the {@link ResultTransformer} to use during projection
	 * @return {@code this} for method chaining
	 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@link SearchSession}
	 * using {@link org.hibernate.search.mapper.orm.Search#session(EntityManager)},
	 * create a search query with {@link SearchSession#search(Class)},
	 * and define your projections using {@link SearchQuerySelectStep#select(Function)}.
	 * See in particular the composite projection, which allows applying a function to another projection:
	 * {@link SearchProjectionFactory#composite()}.
	 * Refer to the <a href="https://hibernate.org/search/documentation/migrate/6.0/">migration guide</a> for more information.
	 */
	@Deprecated
	FullTextQuery setResultTransformer(ResultTransformer transformer);

	/**
	 * Return the Lucene {@link org.apache.lucene.search.Explanation}
	 * object describing the score computation for the matching object/document
	 * in the current query
	 *
	 * @param entityId The identifier of the entity whose match should be explained.
	 *
	 * @return Lucene {@link Explanation}
	 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@link SearchSession}
	 * using {@link org.hibernate.search.mapper.orm.Search#session(EntityManager)},
	 * create a search query with {@link SearchSession#search(Class)},
	 * convert it to a {@link org.hibernate.search.backend.lucene.search.query.LuceneSearchQuery}
	 * by passing {@link LuceneExtension#get()} to {@link SearchQuery#extension(SearchQueryExtension)},
	 * and get the explanation using {@link org.hibernate.search.backend.lucene.search.query.LuceneSearchQuery#explain(Object)}.
	 * Note the {@code explain} methods now expect an entity ID, not the internal Lucene docId.
	 * Refer to the <a href="https://hibernate.org/search/documentation/migrate/6.0/">migration guide</a> for more information.
	 */
	@Deprecated
	Explanation explain(Object entityId);

	/**
	 * Limit the time used by Hibernate Search to execute the query. When the limit is reached, results already
	 * fetched are returned. This time limit is a best effort. The query will likely run for longer than the
	 * provided time.
	 *
	 * The time limit only applies to the interactions between Hibernate Search and Lucene. In other words,
	 * a query to the database will not be limited.
	 *
	 * If the limit is reached and all results are not yet fetched, {@link #hasPartialResults()} returns true.
	 *
	 * @param timeout time out period
	 * @param timeUnit time out unit
	 * @return {@code this} for method chaining
	 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@link SearchSession}
	 * using {@link org.hibernate.search.mapper.orm.Search#session(EntityManager)},
	 * create a search query with {@link SearchSession#search(Class)},
	 * set a "truncation" timeout using {@link SearchQueryOptionsStep#truncateAfter(long, TimeUnit)},
	 * and get the {@link SearchResult} with {@link SearchQuery#fetch(Integer)}.
	 * You'll be able to check whether the result is partial or not using {@link SearchResult#timedOut()}.
	 * Refer to the <a href="https://hibernate.org/search/documentation/migrate/6.0/">migration guide</a> for more information.
	 */
	FullTextQuery limitExecutionTimeTo(long timeout, TimeUnit timeUnit);

	/**
	 * @return When using {@link #limitExecutionTimeTo(long, java.util.concurrent.TimeUnit)} }, returns {@code true}
	 *         if partial results are returned (ie if the time limit has been reached
	 *         and the result fetching process has been terminated.
	 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@link SearchSession}
	 * using {@link org.hibernate.search.mapper.orm.Search#session(EntityManager)},
	 * create a search query with {@link SearchSession#search(Class)},
	 * set a "truncation" timeout using {@link SearchQueryOptionsStep#truncateAfter(long, TimeUnit)},
	 * and get the {@link SearchResult} with {@link SearchQuery#fetch(Integer)}.
	 * You'll be able to check whether the result is partial or not using {@link SearchResult#timedOut()}.
	 * Refer to the <a href="https://hibernate.org/search/documentation/migrate/6.0/">migration guide</a> for more information.
	 */
	@Deprecated
	boolean hasPartialResults();

	/**
	 * Refine the strategies used to load entities.
	 *
	 * The lookup method defines whether or not to lookup first in the second level cache or the persistence context
	 * before trying to initialize objects from the database. Defaults to SKIP.
	 *
	 * The database retrieval method defines how objects are loaded from the database. Defaults to QUERY.
	 *
	 * Note that Hibernate Search can deviate from these choices when it makes sense.
	 *
	 * @param lookupMethod lookup method
	 * @param retrievalMethod how to initilize an object
	 * @return {@code this} for method chaining
	 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@link SearchSession}
	 * using {@link org.hibernate.search.mapper.orm.Search#session(EntityManager)},
	 * create a search query with {@link SearchSession#search(Class)},
	 * and define your loading options using {@link SearchQueryOptionsStep#loading(Consumer)}.
	 * To set the equivalent to {@link ObjectLookupMethod} in Hibernate Search 6,
	 * use {@link SearchLoadingOptionsStep#cacheLookupStrategy(EntityLoadingCacheLookupStrategy)}.
	 * Refer to the <a href="https://hibernate.org/search/documentation/migrate/6.0/">migration guide</a> for more information.
	 */
	FullTextQuery initializeObjectsWith(ObjectLookupMethod lookupMethod, DatabaseRetrievalMethod retrievalMethod);

	/**
	 * {@inheritDoc}
	 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@link SearchSession}
	 * using {@link org.hibernate.search.mapper.orm.Search#session(EntityManager)},
	 * create a search query with {@link SearchSession#search(Class)},
	 * and pass the hit limit ("maxResults") when fetching results using
	 * {@link SearchQuery#fetch(Integer)}, {@link SearchQuery#fetch(Integer, Integer)},
	 * {@link SearchQuery#fetchHits(Integer)} or {@link SearchQuery#fetch(Integer, Integer)}.
	 */
	@Override
	FullTextQuery setMaxResults(int maxResult);

	/**
	 * {@inheritDoc}
	 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@link SearchSession}
	 * using {@link org.hibernate.search.mapper.orm.Search#session(EntityManager)},
	 * create a search query with {@link SearchSession#search(Class)},
	 * and pass the hit offset ("firstResult") when fetching results using
	 * {@link SearchQuery#fetch(Integer)}, {@link SearchQuery#fetch(Integer, Integer)},
	 * {@link SearchQuery#fetchHits(Integer)} or {@link SearchQuery#fetch(Integer, Integer)}.
	 */
	@Override
	FullTextQuery setFirstResult(int var1);

	@Override
	FullTextQuery setHint(String hintName, Object value);

	@Override
	FullTextQuery setFlushMode(FlushModeType flushMode);

	/**
	 * Define a timeout period for a given unit of time.
	 * Note that this is time out is on a best effort basis.
	 * When the query goes beyond the timeout, a {@link jakarta.persistence.QueryTimeoutException} is raised.
	 *
	 * @param timeout time out period
	 * @param timeUnit time out unit
	 *
	 * @return {@code this} to allow method chaining
	 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@link SearchSession}
	 * using {@link org.hibernate.search.mapper.orm.Search#session(EntityManager)},
	 * create a search query with {@link SearchSession#search(Class)},
	 * and set a "failure" timeout using {@link SearchQueryOptionsStep#failAfter(long, TimeUnit)}.
	 * Refer to the <a href="https://hibernate.org/search/documentation/migrate/6.0/">migration guide</a> for more information.
	 */
	@Deprecated
	FullTextQuery setTimeout(long timeout, TimeUnit timeUnit);

}
