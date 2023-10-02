/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.highlighter.impl;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.highlighter.dsl.spi.AbstractSearchHighlighterFactory;
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterBuilder;

public class ElasticsearchSearchHighlighterFactory
		extends AbstractSearchHighlighterFactory<ElasticsearchSearchIndexScope<?>> {

	public ElasticsearchSearchHighlighterFactory(ElasticsearchSearchIndexScope<?> scope) {
		super( scope );
	}

	@Override
	protected SearchHighlighterBuilder highlighterBuilder(
			ElasticsearchSearchIndexScope<?> scope) {
		return new ElasticsearchSearchHighlighterImpl.Builder( scope );
	}
}
