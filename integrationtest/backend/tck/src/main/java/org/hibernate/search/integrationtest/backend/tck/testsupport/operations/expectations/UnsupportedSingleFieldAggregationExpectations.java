/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.operations.expectations;

import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;

public interface UnsupportedSingleFieldAggregationExpectations {

	String aggregationName();

	void trySetup(SearchAggregationFactory factory, String fieldPath);

}
