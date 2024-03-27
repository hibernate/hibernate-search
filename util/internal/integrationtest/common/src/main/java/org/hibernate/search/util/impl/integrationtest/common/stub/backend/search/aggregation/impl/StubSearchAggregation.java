/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.aggregation.impl;

import java.util.Map;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.AbstractStubSearchQueryElementFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexNodeContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexScope;

public class StubSearchAggregation<A> implements SearchAggregation<A> {

	private StubSearchAggregation() {
	}

	public void simulateBuild() {
		// No-op, just simulates a call on this object
	}

	public static class TermsFactory extends AbstractStubSearchQueryElementFactory<TermsAggregationBuilder.TypeSelector> {
		@Override
		public TermsAggregationBuilder.TypeSelector create(StubSearchIndexScope scope,
				StubSearchIndexNodeContext node) {
			return new TermsTypeSelector();
		}
	}

	public static class RangeFactory extends AbstractStubSearchQueryElementFactory<RangeAggregationBuilder.TypeSelector> {
		@Override
		public RangeAggregationBuilder.TypeSelector create(StubSearchIndexScope scope,
				StubSearchIndexNodeContext node) {
			return new RangeTypeSelector();
		}
	}

	public static class TermsTypeSelector implements TermsAggregationBuilder.TypeSelector {
		@Override
		public <V> TermsBuilder<V> type(Class<V> expectedType, ValueConvert convert) {
			return new TermsBuilder<>();
		}
	}

	public static class RangeTypeSelector implements RangeAggregationBuilder.TypeSelector {
		@Override
		public <V> RangeBuilder<V> type(Class<V> expectedType, ValueConvert convert) {
			return new RangeBuilder<>();
		}
	}

	static class TermsBuilder<K> implements TermsAggregationBuilder<K> {

		@Override
		public void orderByCountDescending() {
			// No-op
		}

		@Override
		public void orderByCountAscending() {
			// No-op
		}

		@Override
		public void orderByTermDescending() {
			// No-op
		}

		@Override
		public void orderByTermAscending() {
			// No-op
		}

		@Override
		public void minDocumentCount(int minDocumentCount) {
			// No-op
		}

		@Override
		public void maxTermCount(int maxTermCount) {
			// No-op
		}

		@Override
		public void filter(SearchPredicate filter) {
			// No-op
		}

		@Override
		public SearchAggregation<Map<K, Long>> build() {
			return new StubSearchAggregation<>();
		}
	}

	static class RangeBuilder<K> implements RangeAggregationBuilder<K> {

		@Override
		public void range(Range<? extends K> range) {
			// No-op
		}

		@Override
		public void filter(SearchPredicate filter) {
			// No-op
		}

		@Override
		public SearchAggregation<Map<Range<K>, Long>> build() {
			return new StubSearchAggregation<>();
		}
	}
}
