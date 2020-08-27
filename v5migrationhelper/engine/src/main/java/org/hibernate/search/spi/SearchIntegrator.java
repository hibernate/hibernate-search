/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spi;

import org.hibernate.search.query.dsl.QueryContextBuilder;
import org.hibernate.search.query.engine.spi.HSQuery;

import org.apache.lucene.search.Query;

/**
 * This contract gives access to lower level APIs of Hibernate Search for
 * frameworks integrating with it.
 * Frameworks should not expose this as public API though, but expose a simplified view; for
 * example the Hibernate Search ORM module expose the {@code org.hibernate.search.SearchFactory} contract to
 * its clients.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public interface SearchIntegrator {

	/**
	 * Return an Hibernate Search query object.
	 * <p>This method DOES support non-Lucene backends (e.g. Elasticsearch).
	 * <p>The returned object uses fluent APIs to define additional query settings.
	 *
	 * @param fullTextQuery the full-text engine query
	 * @param entityTypes the targeted entity types
	 * @return an Hibernate Search query object
	 */
	HSQuery createHSQuery(Query fullTextQuery, Class<?>... entityTypes);

	/**
	 * @return return a query builder providing a fluent API to create Lucene queries
	 */
	QueryContextBuilder buildQueryBuilder();

}
