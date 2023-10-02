/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.query.dsl;

import org.hibernate.search.backend.elasticsearch.search.predicate.dsl.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryWhereStep;

public interface ElasticsearchSearchQueryWhereStep<H, LOS>
		extends SearchQueryWhereStep<ElasticsearchSearchQueryOptionsStep<H, LOS>, H, LOS, ElasticsearchSearchPredicateFactory> {

}
