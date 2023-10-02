/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import org.hibernate.search.backend.elasticsearch.search.predicate.impl.PredicateRequestContext;

public interface AggregationRequestContext {

	PredicateRequestContext getRootPredicateContext();

}
