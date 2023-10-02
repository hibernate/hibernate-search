/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.query.engine.spi;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.spatial.Coordinates;

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;

/**
 * Defines and executes an Hibernate Search query (wrapping a Lucene query).
 * Uses fluent APIs to define the query and offer a few alternatives
 * on how to execute the query.
 *
 * This object is not meant to be thread safe.
 * The typical usage is as follow
 * <pre>
 * {@code  //get query object
 * HSQuery query = searchIntegrator.createHSQuery();
 * //configure query object
 * query
 * .luceneQuery( luceneQuery )
 * .timeoutExceptionFactory( exceptionFactory )
 * .targetedEntities( Arrays.asList( classes ) );
 *
 * [...]
 *
 * //start timeout counting
 * query.getTimeoutManager().start();
 *
 * //query execution and use
 * List<EntityInfo> entityInfos = query.getEntityInfos();
 * [...]
 *
 * //done with the core of the query
 * query.getTimeoutManager().stop();
 * }
 * </pre>
 *
 * @author Emmanuel Bernard
 * @deprecated This class will be removed without replacement. Use actual API instead.
 */
@Deprecated
public interface HSQuery extends ProjectionConstants {

	/**
	 * Lets Lucene sort the results. This is useful when you have
	 * different sort requirements than the default Lucene ranking.
	 * Without Lucene sorting you would have to retrieve the full result set and
	 * order the Hibernate objects.
	 *
	 * @param sort The Lucene sort object.
	 * @return {@code this}  to allow for method chaining
	 */
	HSQuery sort(Sort sort);

	/**
	 * Defines the Lucene field names projected and returned in a query result
	 * Each field is converted back to it's object representation, an Object[] being returned for each "row"
	 * (similar to an HQL or a Criteria API projection).
	 * <p>
	 * A projectable field must be stored in the Lucene index and use a two-way field bridge
	 * Unless notified in their JavaDoc, all built-in bridges are two-way. All @DocumentId fields are projectable by design.
	 * <p>
	 * If the projected field is not a projectable field, null is returned in the object[]
	 *
	 * @param fields the projected field names
	 * @return {@code this}  to allow for method chaining
	 */
	HSQuery projection(String... fields);

	/**
	 * Set the first element to retrieve. If not set, elements will be
	 * retrieved beginning from element {@code 0}.
	 *
	 * @param firstResult a element number, numbered from {@code 0}
	 * @return {@code this}  to allow for method chaining
	 */
	HSQuery firstResult(int firstResult);

	/**
	 * Set the maximum number of elements to retrieve. If not set,
	 * there is no limit to the number of elements retrieved.
	 *
	 * @param maxResults the maximum number of elements
	 *
	 * @return {@code this} in order to allow method chaining
	 */
	HSQuery maxResults(Integer maxResults);

	/**
	 * @return The last value passed to {@link #maxResults(Integer)}, or {@code null}.
	 */
	Integer maxResults();

	/**
	 * @return the targeted entity types
	 */
	Set<Class<?>> getTargetedEntities();

	/**
	 * @return the projected field names
	 */
	String[] getProjectedFields();

	/**
	 * @param timeout Timeout value.
	 * @param timeUnit Timeout unit.
	 * @see org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep#failAfter(long, TimeUnit)
	 */
	void failAfter(long timeout, TimeUnit timeUnit);

	/**
	 * @param timeout Timeout value.
	 * @param timeUnit Timeout unit.
	 * @see org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep#truncateAfter(long, TimeUnit)
	 */
	void truncateAfter(long timeout, TimeUnit timeUnit);

	/**
	 * @return return the manager for all faceting related operations
	 */
	FacetManager getFacetManager();

	/**
	 * @return the underlying Lucene query (if any)
	 */
	Query getLuceneQuery();

	/**
	 * @return a String representation of the underlying query
	 */
	String getQueryString();

	/**
	 * Execute the Lucene query and return the hits.
	 * @return list of hits.
	 */
	List<?> fetch();

	/**
	 * @return {@code true} if the last call to {@link #fetch()}
	 * returned a {@link #truncateAfter(long, TimeUnit) truncated} list of hits,
	 * {@code false} otherwise.
	 */
	boolean hasPartialResults();

	/**
	 * @return the number of hits for this search
	 *         <p>
	 *         Caution:
	 *         The number of results might be slightly different from
	 *         what the object source returns if the index is
	 *         not in sync with the store at the time of query.
	 */
	int getResultSize();

	/**
	 * Execute the Lucene query continuously, as a {@link SearchScroll}.
	 * @param chunkSize The size of chunks.
	 * @return the scroll (must eventually be closed).
	 */
	SearchScroll<?> scroll(int chunkSize);

	/**
	 * Return the Lucene {@link Explanation}
	 * object describing the score computation for the matching object/document
	 * in the current query
	 *
	 * @param entityId The identifier of the entity whose match should be explained.
	 * @return Lucene Explanation
	 */
	Explanation explain(Object entityId);

	/**
	 * <p>setSpatialParameters.</p>
	 *
	 * @param center center of the spatial search
	 * @param fieldName name ot the spatial field
	 * @return {@code this}  to allow for method chaining
	 */
	HSQuery setSpatialParameters(Coordinates center, String fieldName);

	HSQuery tupleTransformer(TupleTransformer tupleTransformer);

}
