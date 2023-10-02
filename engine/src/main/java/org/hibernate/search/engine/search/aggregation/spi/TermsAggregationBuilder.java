/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.spi;

import java.util.Map;

import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.SearchPredicate;

public interface TermsAggregationBuilder<K> extends SearchAggregationBuilder<Map<K, Long>> {

	interface TypeSelector {
		<K> TermsAggregationBuilder<K> type(Class<K> expectedType, ValueConvert convert);
	}

	void filter(SearchPredicate filter);

	void orderByCountDescending();

	void orderByCountAscending();

	void orderByTermDescending();

	void orderByTermAscending();

	void minDocumentCount(int minDocumentCount);

	void maxTermCount(int maxTermCount);

}
