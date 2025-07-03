/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.spi;

import java.util.Map;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.predicate.SearchPredicate;

public interface TermsAggregationBuilder<K, V> extends SearchAggregationBuilder<Map<K, V>> {

	interface TypeSelector {
		<K> TermsAggregationBuilder<K, Long> type(Class<K> expectedType, ValueModel valueModel);
	}

	void filter(SearchPredicate filter);

	void orderByCountDescending();

	void orderByCountAscending();

	void orderByTermDescending();

	void orderByTermAscending();

	void minDocumentCount(int minDocumentCount);

	void maxTermCount(int maxTermCount);

	<T> TermsAggregationBuilder<K, T> withValue(SearchAggregation<T> aggregation);

}
