/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search;

import java.util.concurrent.TimeUnit;

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Sort;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.filter.FullTextFilter;
import org.hibernate.search.query.DatabaseRetrievalMethod;
import org.hibernate.search.query.ObjectLookupMethod;
import org.hibernate.search.query.engine.spi.FacetManager;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.transform.ResultTransformer;

/**
 * The base interface for Lucene powered searches.
 *
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
public interface FullTextQuery extends Query, ProjectionConstants, QueryImplementor {

	/**
	 * Allows to let lucene sort the results. This is useful when you have
	 * additional sort requirements on top of the default lucene ranking.
	 * Without lucene sorting you would have to retrieve the full result set and
	 * order the hibernate objects.
	 *
	 * @param sort The lucene sort object.
	 *
	 * @return this for method chaining
	 */
	FullTextQuery setSort(Sort sort);

	/**
	 * Allows to set a single Lucene filter.
	 * @deprecated: use the {@link org.hibernate.search.annotations.FullTextFilterDef} approach,
	 *     or use a {@link org.apache.lucene.search.BooleanQuery} and add a clause using
	 *     {@link org.apache.lucene.search.BooleanClause.Occur#FILTER}.
	 *
	 * @param filter The Lucene filter.
	 *
	 * @return this for method chaining
	 * @see org.apache.lucene.search.Filter
	 */
	@Deprecated
	FullTextQuery setFilter(Filter filter);

	/**
	 * @return the number of hits for this search.
	 *         <p>
	 *         Caution:
	 *         The number of results might be slightly different from
	 *         <code>list().size()</code> because list() if the index is
	 *         not in sync with the database at the time of query.
	 */
	int getResultSize();

	/**
	 * Defines the Database Query used to load the Lucene results.
	 * Useful to load a given object graph by refining the fetch modes.
	 * <ul>
	 * <li>No projection ({@code criteria.setProjection()}) allowed, the root entity must be the only returned type</li>
	 * <li>No where restriction can be defined either</li>
	 * </ul>
	 *
	 * @param criteria Hibernate criteria query used to load results
	 *
	 * @return {@code this} for method chaining
	 */
	FullTextQuery setCriteriaQuery(Criteria criteria);

	/**
	 * Defines the Lucene field names projected and returned in a query result
	 * Each field is converted back to it's object representation, an Object[] being returned for each "row"
	 * (similar to an HQL or a Criteria API projection).
	 * <p>
	 * A projectable field must be stored in the Lucene index and use a {@link org.hibernate.search.bridge.TwoWayFieldBridge}
	 * Unless notified in their JavaDoc, all built-in bridges are two-way. All @DocumentId fields are projectable by design.
	 * <p>
	 * If the projected field is not a projectable field, null is returned in the object[]
	 *
	 * @param fields list of field names to project on
	 *
	 * @return {@code this} for method chaining
	 */
	FullTextQuery setProjection(String... fields);

	/**
	 * Defines the center of the spatial search for this query to project distance in results
	 *
	 * @param latitude latitude of the search center
	 * @param longitude longitude of the search center
	 * @param fieldName name of the spatial field
	 *
	 * @return {@code this} for method chaining
	 */
	FullTextQuery setSpatialParameters(double latitude, double longitude, String fieldName);

	/**
	 * Defines the center of the spatial search for this query to project distance in results
	 *
	 * @param center the search center
	 * @param fieldName name of the spatial field
	 *
	 * @return {@code this} for method chaining
	 */
	FullTextQuery setSpatialParameters(Coordinates center, String fieldName);

	/**
	 * Enable a given filter by its name.
	 *
	 * @param name the name of the filter to enable
	 * @return Returns a {@code FullTextFilter} object that allows filter parameter injection
	 * @throws org.hibernate.search.exception.SearchException in case the filter with the specified name is not defined
	 */
	FullTextFilter enableFullTextFilter(String name);

	/**
	 * Disable a given filter by its name.
	 *
	 * @param name the name of the filter to disable.
	 */
	void disableFullTextFilter(String name);

	/**
	 * @return return the manager for all faceting related operations
	 */
	FacetManager getFacetManager();

	/**
	 * Return the Lucene {@link org.apache.lucene.search.Explanation}
	 * object describing the score computation for the matching object/document
	 * in the current query
	 *
	 * @param documentId Lucene Document id to be explain. This is NOT the object id
	 *
	 * @return An Lucene {@code Explanation} instance
	 */
	Explanation explain(int documentId);

	/**
	 * {@link Query#setFirstResult}
	 */
	@Override
	FullTextQuery setFirstResult(int firstResult);

	/**
	 * {@link Query#setMaxResults}
	 */
	@Override
	FullTextQuery setMaxResults(int maxResults);

	/**
	 * Defines scrollable result fetch size as well as the JDBC fetch size
	 */
	@Override
	FullTextQuery setFetchSize(int i);

	/**
	 * defines a result transformer used during projection, the Aliases provided are the projection aliases.
	 */
	@Override
	FullTextQuery setResultTransformer(ResultTransformer transformer);

	/**
	 * @param <T> the type of the unwrapped object
	 * @param type the type to unwrap
	 *
	 * @return the underlying type if possible. If not possible to unwrap to the given type an
	 *         {@code IllegalArgumentException} is thrown. Supported types are:
	 *         <ul>
	 *         <li>org.apache.lucene.search.Query the underlying lucene query</li>
	 *         </ul>
	 */
	<T> T unwrap(Class<T> type);

	/**
	 * Define a timeout period for a given unit of time.
	 * Note that this is time out is on a best effort basis.
	 * When the query goes beyond the timeout, a {@link org.hibernate.QueryTimeoutException} is raised.
	 *
	 * @param timeout time out period
	 * @param timeUnit time out unit
	 *
	 * @return {@code this} to allow method chaining
	 */
	FullTextQuery setTimeout(long timeout, TimeUnit timeUnit);

	/**
	 * *Experimental* API, subject to change or removal
	 *
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
	 *
	 * @return {@code this} to allow method chaining
	 */
	FullTextQuery limitExecutionTimeTo(long timeout, TimeUnit timeUnit);

	/**
	 * <b>Experimental</b> API, subject to change or removal
	 *
	 * @return When using {@link #limitExecutionTimeTo(long, java.util.concurrent.TimeUnit)} }, returns {@code true}
	 *         if partial results are returned (ie if the time limit has been reached
	 *         and the result fetching process has been terminated.
	 */
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
	 * @param lookupMethod the lookuip strategy
	 * @param retrievalMethod the retrieval strategy
	 * @return {@code this} to allow method chaining
	 */
	FullTextQuery initializeObjectsWith(ObjectLookupMethod lookupMethod, DatabaseRetrievalMethod retrievalMethod);
}
