/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.highlighter.impl;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.highlighter.dsl.spi.AbstractSearchHighlighterFactory;
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterBuilder;

public class LuceneSearchHighlighterFactory extends AbstractSearchHighlighterFactory<LuceneSearchIndexScope<?>> {

	public LuceneSearchHighlighterFactory(LuceneSearchIndexScope<?> scope) {
		super( scope );
	}

	@Override
	protected SearchHighlighterBuilder highlighterBuilder(LuceneSearchIndexScope<?> scope) {
		return new LuceneAbstractSearchHighlighter.Builder( scope );
	}
}
