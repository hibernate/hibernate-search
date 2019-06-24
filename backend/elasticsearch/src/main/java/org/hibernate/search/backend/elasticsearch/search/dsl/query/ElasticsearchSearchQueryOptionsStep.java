/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.query;

import org.hibernate.search.backend.elasticsearch.search.dsl.sort.ElasticsearchSearchSortFactory;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchFetchable;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchQuery;
import org.hibernate.search.engine.search.dsl.query.SearchQueryOptionsStep;

public interface ElasticsearchSearchQueryOptionsStep<H>
		extends SearchQueryOptionsStep<ElasticsearchSearchQueryOptionsStep<H>, H, ElasticsearchSearchSortFactory>,
				ElasticsearchSearchFetchable<H> {

	@Override
	ElasticsearchSearchQuery<H> toQuery();
}
