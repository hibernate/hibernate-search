/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.sort.impl;

import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.engine.search.sort.spi.CompositeSortBuilder;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.search.sort.spi.ScoreSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.AbstractStubSearchQueryElementFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexNodeContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexScope;

public class StubSearchSort implements SearchSort {

	public static StubSearchSort from(SearchSort sort) {
		return (StubSearchSort) sort;
	}

	private StubSearchSort() {
	}

	public void simulateBuild() {
		// No-op, just simulates a call on this object
	}

	public static class Factory extends AbstractStubSearchQueryElementFactory<Builder> {
		@Override
		public Builder create(StubSearchIndexScope scope, StubSearchIndexNodeContext node) {
			return new Builder();
		}
	}

	public static class Builder
			implements ScoreSortBuilder,
			FieldSortBuilder, DistanceSortBuilder, CompositeSortBuilder {

		@Override
		public SearchSort build() {
			return new StubSearchSort();
		}

		@Override
		public void missingFirst() {
			// No-op
		}

		@Override
		public void missingLast() {
			// No-op
		}

		@Override
		public void missingHighest() {
			// No-op
		}

		@Override
		public void missingLowest() {
			// No-op
		}

		@Override
		public void missingAs(GeoPoint value) {
			// No-op
		}

		@Override
		public void missingAs(Object value, ValueConvert convert) {
			// No-op
		}

		@Override
		public void center(GeoPoint center) {
			// No-op
		}

		@Override
		public void order(SortOrder order) {
			// No-op
		}

		@Override
		public void mode(SortMode mode) {
			// No-op
		}

		@Override
		public void filter(SearchPredicate clauseBuilder) {
			// No-op, just simulates a call on this object
		}

		@Override
		public void add(SearchSort sort) {
			// No-op
		}
	}
}
