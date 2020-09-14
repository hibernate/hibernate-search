/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jpa;

import java.util.concurrent.TimeUnit;

import javax.persistence.FlushModeType;
import javax.persistence.Query;

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Sort;

import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.query.DatabaseRetrievalMethod;
import org.hibernate.search.query.ObjectLookupMethod;
import org.hibernate.search.query.engine.spi.FacetManager;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.transform.ResultTransformer;

/**
 * The base interface for full-text queries using the JPA API ({@link javax.persistence.Query}).
 *
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
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
	 */
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
	 */
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
	 * @return return the manager for all faceting related operations
	 */
	FacetManager getFacetManager();

	/**
	 * Defines a result transformer used during projection
	 *
	 * @param transformer the {@link ResultTransformer} to use during projection
	 * @return {@code this} for method chaining
	 */
	FullTextQuery setResultTransformer(ResultTransformer transformer);

	/**
	 * Return the Lucene {@link org.apache.lucene.search.Explanation}
	 * object describing the score computation for the matching object/document
	 * in the current query
	 *
	 * @param entityId The identifier of the entity whose match should be explained.
	 *
	 * @return Lucene {@link Explanation}
	 */
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
	 */
	FullTextQuery limitExecutionTimeTo(long timeout, TimeUnit timeUnit);

	/**
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
	 * @param lookupMethod lookup method
	 * @param retrievalMethod how to initilize an object
	 * @return {@code this} for method chaining
	 */
	FullTextQuery initializeObjectsWith(ObjectLookupMethod lookupMethod, DatabaseRetrievalMethod retrievalMethod);

	@Override
	FullTextQuery setMaxResults(int maxResult);

	@Override
	FullTextQuery setFirstResult(int var1);

	@Override
	FullTextQuery setHint(String hintName, Object value);

	@Override
	FullTextQuery setFlushMode(FlushModeType flushMode);

	/**
	 * Define a timeout period for a given unit of time.
	 * Note that this is time out is on a best effort basis.
	 * When the query goes beyond the timeout, a {@link javax.persistence.QueryTimeoutException} is raised.
	 *
	 * @param timeout time out period
	 * @param timeUnit time out unit
	 *
	 * @return {@code this} to allow method chaining
	 */
	FullTextQuery setTimeout(long timeout, TimeUnit timeUnit);

}
