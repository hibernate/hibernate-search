/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.lucene.search.query.LuceneSearchResult;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.query.SearchResultTotal;
import org.hibernate.search.engine.search.query.spi.SimpleSearchResult;

import org.apache.lucene.search.TopDocs;

class LuceneSearchResultImpl<H> extends SimpleSearchResult<H>
		implements LuceneSearchResult<H> {

	private final TopDocs topDocs;

	LuceneSearchResultImpl(SearchResultTotal resultTotal, List<H> hits, Map<AggregationKey<?>, ?> aggregationResults,
			Duration took, Boolean timedOut, TopDocs topDocs) {
		super( resultTotal, hits, aggregationResults, took, timedOut );
		this.topDocs = topDocs;
	}

	@Override
	public TopDocs topDocs() {
		return topDocs;
	}
}
