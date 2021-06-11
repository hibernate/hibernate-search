/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexSchemaElementContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexScope;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchQueryElementFactory;

public class StubSearchSort implements SearchSort {

	public static StubSearchSort from(SearchSort sort) {
		return (StubSearchSort) sort;
	}

	private StubSearchSort() {
	}

	void simulateBuild() {
		// No-op, just simulates a call on this object
	}

	public static class Factory implements StubSearchQueryElementFactory<Builder> {
		@Override
		public Builder create(StubSearchIndexScope scope, StubSearchIndexSchemaElementContext element) {
			return new Builder();
		}
	}

	public static class Builder implements ScoreSortBuilder,
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
