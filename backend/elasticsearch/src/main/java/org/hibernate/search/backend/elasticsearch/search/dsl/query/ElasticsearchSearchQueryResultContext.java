/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.query;

import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;

public interface ElasticsearchSearchQueryResultContext<Q>
		extends SearchQueryResultContext<ElasticsearchSearchQueryContext<Q>, Q> {

	// FIXME make predicate(Function) accept a function that expects Elasticsearch-specific input

}
