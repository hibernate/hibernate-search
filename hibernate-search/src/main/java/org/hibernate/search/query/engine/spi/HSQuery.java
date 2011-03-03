/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.query.engine.spi;

import java.util.List;
import java.util.Set;

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;

import org.hibernate.search.FullTextFilter;
import org.hibernate.search.ProjectionConstants;
import org.hibernate.search.engine.DocumentExtractor;
import org.hibernate.search.engine.EntityInfo;
import org.hibernate.search.engine.SearchFactoryImplementor;

/**
 * Defines and execute an Hibernate Search query (wrapping a Lucene query).
 * Uses fluent APIs to define the query and offer a few alternative
 * on how to execute the query.
 *
 * This object is not meant to be thread safe.
 * The typical usage is as follow
 * <code>
 *     //get query object
 *     HSQuery query = searchFactoryIngegrator.createHSQuery();
 *     //configure query object
 *     query
 *     	.luceneQuery( luceneQuery )
 *     	.timeoutExceptionFactory( exceptionFactory )
 *     	.targetedEntities( Arrays.asList( classes ) );
 *
 *     [...]
 *
 *     //start timeout counting
 *     query.getTimeoutManager().start();
 *
 *     //query execution and use
 *     List<EntityInfo> entityInfos = query.getEntityInfos();
 *     [...]
 *
 *     //done with the core of the query
 *     query.getTimeoutManager().stop();
 *  </code>
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public interface HSQuery extends ProjectionConstants {
	/**
	 * Defines the underlying Lucene query
	 */
	HSQuery luceneQuery(Query query);

	/**
	 * Defines the targeted entities. This helps to reduce indexes targeted.
	 */
	HSQuery targetedEntities(List<Class<?>> classes);

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
	HSQuery sort(Sort sort);

	/**
	 * Allows to use lucene filters.
	 * Semi-deprecated? a preferred way is to use the @FullTextFilterDef approach
	 *
	 * @param filter The lucene filter.
	 *
	 * @return this for method chaining
	 */
	HSQuery filter(Filter filter);

	/**
	 * Define the timeout exception factory to customize the exception returned by the user.
	 * Defaults to returning {@link org.hibernate.search.query.engine.QueryTimeoutException}
	 */
	HSQuery timeoutExceptionFactory(TimeoutExceptionFactory exceptionFactory);

	/**
	 * Defines the Lucene field names projected and returned in a query result
	 * Each field is converted back to it's object representation, an Object[] being returned for each "row"
	 * (similar to an HQL or a Criteria API projection).
	 * <p/>
	 * A projectable field must be stored in the Lucene index and use a {@link org.hibernate.search.bridge.TwoWayFieldBridge}
	 * Unless notified in their JavaDoc, all built-in bridges are two-way. All @DocumentId fields are projectable by design.
	 * <p/>
	 * If the projected field is not a projectable field, null is returned in the object[]
	 */
	HSQuery projection(String... fields);

	/**
	 * Set the first element to retrieve. If not set, elements will be
	 * retrieved beginning from element <tt>0</tt>.
	 * @param firstResult a element number, numbered from <tt>0</tt>
	 */
	HSQuery firstResult(int firstResult);

	/**
	 * Set the maximum number of elements to retrieve. If not set,
	 * there is no limit to the number of elements retrieved.
	 * @param maxResults the maximum number of elements
	 */
	HSQuery maxResults(Integer maxResults);

	/**
	 * Return the targeted entity types
	 */
	List<Class<?>> getTargetedEntities();

	/**
	 * WTF does that do exactly
	 */
	Set<Class<?>> getIndexedTargetedEntities();

	/**
	 * return the projected fields
	 */
	String[] getProjectedFields();

	/**
	 * Return the timeout manager
	 * Make sure to wrap your HSQuery usage around a timeoutManager.start() / .stop()
	 */
	TimeoutManager getTimeoutManager();

	/**
	 * The underlying Lucene query
	 */
	Query getLuceneQuery();

	/**
	 * Execute the Lucene query and return the list of EntityInfos populated with
	 * metadata and projection.
	 * THIS if projected is *not* populated:
	 * it is the responsibility of the object source integration
	 */
	List<EntityInfo> getEntityInfos();

	/**
	 * Execute the Lucene query and return a traversable object over the results.
	 * Results are lazily fetched.
	 * THIS if projected is *not* populated:
	 * it is the responsibility of the object source integration
	 * DocumentExtractor *must* be closed by the caller to release Lucene resources.
	 */
	DocumentExtractor getDocumentExtractor();

	/**
	 * Returns the number of hits for this search
	 * <p/>
	 * Caution:
	 * The number of results might be slightly different from
	 * what the object source retuns if the index is
	 * not in sync with the store at the time of query.
	 */
	int getResultSize();

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
	 * Enable a given filter by its name. Returns a FullTextFilter object that allows filter parameter injection
	 */
	FullTextFilter enableFullTextFilter(String name);

	/**
	 * Disable a given filter by its name
	 */
	void disableFullTextFilter(String name);

	/**
	 * @deprecated should be at most SearchFactoryIntegrator, preferably removed altogether
	 */
	SearchFactoryImplementor getSearchFactoryImplementor();
}
