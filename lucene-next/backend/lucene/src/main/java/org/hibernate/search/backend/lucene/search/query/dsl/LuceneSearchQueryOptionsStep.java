/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.query.dsl;

import org.hibernate.search.backend.lucene.search.aggregation.dsl.LuceneSearchAggregationFactory;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchFetchable;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchQuery;
import org.hibernate.search.backend.lucene.search.sort.dsl.LuceneSearchSortFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;

public interface LuceneSearchQueryOptionsStep<H, LOS>
		extends SearchQueryOptionsStep<
				LuceneSearchQueryOptionsStep<H, LOS>,
				H,
				LOS,
				LuceneSearchSortFactory,
				LuceneSearchAggregationFactory>,
		LuceneSearchFetchable<H> {

	@Override
	LuceneSearchQuery<H> toQuery();

}
