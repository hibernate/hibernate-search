/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 * 
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search;

import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.Explanation;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.transform.ResultTransformer;

/**
 * The base interface for Lucene powered searches.
 *
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
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
	 * <p/>
	 * Caution:
	 * The number of results might be slightly different from
	 * <code>list().size()</code> because list() if the index is
	 * not in sync with the database at the time of query.
	 */
	int getResultSize();

	/**
	 * Defines the Database Query used to load the Lucene results.
	 * Useful to load a given object graph by refining the fetch modes
	 * <p/>
	 * No projection (criteria.setProjection() ) allowed, the root entity must be the only returned type
	 * No where restriction can be defined either.
	 */
	FullTextQuery setCriteriaQuery(Criteria criteria);

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
	FullTextQuery setProjection(String... fields);

	/**
	 * Enable a given filter by its name. Returns a FullTextFilter object that allows filter parameter injection
	 */
	FullTextFilter enableFullTextFilter(String name);

	/**
	 * Disable a given filter by its name
	 */
	void disableFullTextFilter(String name);

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
	 * {link:Query#setFirstResult}
	 */
	FullTextQuery setFirstResult(int firstResult);

	/**
	 * {link:Query#setMaxResults}
	 */
	FullTextQuery setMaxResults(int maxResults);

	/**
	 * Defines scrollable result fetch size as well as the JDBC fetch size
	 */
	FullTextQuery setFetchSize(int i);

	/**
	 * defines a result transformer used during projection, the Aliases provided are the projection aliases.
	 */
	FullTextQuery setResultTransformer(ResultTransformer transformer);

	/**
	 * return the underlying type if possible or IllegalArgumentException otherwise
	 * Supported types are:
	 *  - org.apache.lucene.search.Query the underlying lucene query
	 */
	<T> T unwrap(Class<T> type);
}
