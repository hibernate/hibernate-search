/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jpa;

import javax.persistence.Query;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Sort;

import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.filter.FullTextFilter;
import org.hibernate.search.genericjpa.entity.EntityProvider;
import org.hibernate.search.query.DatabaseRetrievalMethod;
import org.hibernate.search.query.ObjectLookupMethod;
import org.hibernate.search.query.engine.spi.FacetManager;
import org.hibernate.search.spatial.Coordinates;

/**
 * The base interface for lucene powered searches.
 * This extends the JPA Query interface
 *
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 * @author Martin Braun
 */
//TODO return FullTextQuery rather than Query in useful chain methods
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
	 * Allows to use lucene filters.
	 * Semi-deprecated? a preferred way is to use the @FullTextFilterDef approach
	 *
	 * @param filter The lucene filter.
	 *
	 * @return this for method chaining
	 */
	FullTextQuery setFilter(Filter filter);

	/**
	 * Returns the number of hits for this search
	 * <p>
	 * Caution:
	 * The number of results might be slightly different from
	 * <code>getResultList().size()</code> because getResultList()
	 * may be not in sync with the database at the time of query.
	 */
	int getResultSize();

	/**
	 * NOTE: NO setCriteriaQuery(Criteria criteria)!
	 */

	/**
	 * Defines the Lucene field names projected and returned in a query result
	 * Each field is converted back to it's object representation, an Object[] being returned for each "row"
	 * (similar to an HQL or a Criteria API projection).
	 * <p>
	 * A projectable field must be stored in the Lucene index and use a {@link org.hibernate.search.bridge.TwoWayFieldBridge}
	 * Unless notified in their JavaDoc, all built-in bridges are two-way. All @DocumentId fields are projectable by design.
	 * <p>
	 * If the projected field is not a projectable field, null is returned in the object[]
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
	 * Enable a given filter by its name. Returns a FullTextFilter object that allows filter parameter injection
	 */
	FullTextFilter enableFullTextFilter(String name);

	/**
	 * Disable a given filter by its name
	 */
	void disableFullTextFilter(String name);

	/**
	 * @return return the manager for all faceting related operations
	 */
	FacetManager getFacetManager();

	/**
	 * NOTE: NO setResultTransformer()!
	 */

	/**
	 * Return the Lucene {@link org.apache.lucene.search.Explanation}
	 * object describing the score computation for the matching object/document
	 * in the current query
	 *
	 * @param documentId Lucene Document id to be explain. This is NOT the object id
	 *
	 * @return Lucene Explanation
	 */
	Explanation explain(int documentId);

	/**
	 * *Experimental* API, subject to change or removal
	 * <p>
	 * Limit the time used by Hibernate Search to execute the query. When the limit is reached, results already
	 * fetched are returned. This time limit is a best effort. The query will likely run for longer than the
	 * provided time.
	 * <p>
	 * The time limit only applies to the interactions between Hibernate Search and Lucene. In other words,
	 * a query to the database will not be limited.
	 * <p>
	 * If the limit is reached and all results are not yet fetched, {@link #hasPartialResults()} returns true.
	 *
	 * @param timeout time out period
	 * @param timeUnit time out unit
	 */
	FullTextQuery limitExecutionTimeTo(long timeout, TimeUnit timeUnit);

	/**
	 * *Experimental* API, subject to change or removal
	 * <p>
	 * When using {@link #limitExecutionTimeTo(long, java.util.concurrent.TimeUnit)} }, returns true if partial results are returned (ie if the time limit has been reached
	 * and the result fetching process has been terminated.
	 */
	boolean hasPartialResults();

	/**
	 * Refine the strategies used to load entities.
	 * <p>
	 * The lookup method defines whether or not to lookup first in the second level cache or the persistence context
	 * before trying to initialize objects from the database. Defaults to SKIP.
	 * <p>
	 * The database retrieval method defines how objects are loaded from the database. Defaults to QUERY.
	 * <p>
	 * Note that Hibernate Search can deviate from these choices when it makes sense.
	 */
	FullTextQuery initializeObjectsWith(ObjectLookupMethod lookupMethod, DatabaseRetrievalMethod retrievalMethod);

	FullTextQuery entityProvider(EntityProvider entityProvider);

	<T> List<T> queryDto(Class<T> returnedType);

	<T> List<T> queryDto(Class<T> returnedType, String profileName);

}
