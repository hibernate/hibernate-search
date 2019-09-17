/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.aggregation.impl;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;

public class StubSearchAggregation<A> implements SearchAggregation<A> {
	private final StubAggregationBuilder builder;

	StubSearchAggregation(StubAggregationBuilder builder) {
		this.builder = builder;
	}

	StubAggregationBuilder get() {
		return builder;
	}
}
