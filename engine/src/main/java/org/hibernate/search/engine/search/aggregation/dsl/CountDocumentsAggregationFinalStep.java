/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The initial and final step in a "count documents" aggregation definition.
 */
@Incubating
public interface CountDocumentsAggregationFinalStep extends AggregationFinalStep<Long> {

}
