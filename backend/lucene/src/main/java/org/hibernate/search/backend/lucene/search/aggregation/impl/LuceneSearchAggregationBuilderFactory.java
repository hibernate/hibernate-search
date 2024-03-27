/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.aggregation.impl;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilderFactory;

public class LuceneSearchAggregationBuilderFactory
		implements SearchAggregationBuilderFactory {

	@SuppressWarnings("unused")
	public LuceneSearchAggregationBuilderFactory(LuceneSearchIndexScope<?> scope) {
	}

}
