/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.dsl;

import org.hibernate.search.backend.elasticsearch.search.predicate.dsl.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryPredicateStep;

public interface ElasticsearchSearchQueryPredicateStep<H, LOS>
		extends SearchQueryPredicateStep<ElasticsearchSearchQueryOptionsStep<H, LOS>, H, ElasticsearchSearchPredicateFactory> {

}
