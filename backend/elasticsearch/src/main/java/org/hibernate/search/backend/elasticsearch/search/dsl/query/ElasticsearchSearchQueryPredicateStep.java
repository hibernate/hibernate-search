/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.query;

import org.hibernate.search.backend.elasticsearch.search.predicate.dsl.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.engine.search.dsl.query.SearchQueryPredicateStep;

public interface ElasticsearchSearchQueryPredicateStep<H>
		extends SearchQueryPredicateStep<ElasticsearchSearchQueryOptionsStep<H>, H, ElasticsearchSearchPredicateFactory> {

}
