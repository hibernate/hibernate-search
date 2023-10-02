/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import java.time.Duration;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchScrollResult;
import org.hibernate.search.engine.search.query.SearchResultTotal;
import org.hibernate.search.engine.search.query.spi.SimpleSearchScrollResult;

public class ElasticsearchSearchScrollResultImpl<H> extends SimpleSearchScrollResult<H>
		implements ElasticsearchSearchScrollResult<H> {
	public ElasticsearchSearchScrollResultImpl(SearchResultTotal total, boolean hasHits, List<H> hits,
			Duration took, Boolean timedOut) {
		super( total, hasHits, hits, took, timedOut );
	}
}
