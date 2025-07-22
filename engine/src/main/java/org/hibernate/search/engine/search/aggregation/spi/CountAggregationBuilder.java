/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.spi;

public interface CountAggregationBuilder {

	interface TypeSelector {
		CountValuesAggregationBuilder values();

		CountDocumentAggregationBuilder documents();
	}
}
