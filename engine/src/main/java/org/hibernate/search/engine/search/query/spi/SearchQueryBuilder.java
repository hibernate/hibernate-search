/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.spi;

import org.hibernate.search.engine.search.query.SearchQuery;

/**
 * A builder for search queries.
 *
 * @param <H> The type of query results
 * @param <C> The type of query element collector
 */
public interface SearchQueryBuilder<H, C> {

	C toQueryElementCollector();

	void addRoutingKey(String routingKey);

	SearchQuery<H> build();

}
