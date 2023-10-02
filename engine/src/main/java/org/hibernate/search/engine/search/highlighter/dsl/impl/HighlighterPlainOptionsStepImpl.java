/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.highlighter.dsl.impl;

import org.hibernate.search.engine.search.highlighter.dsl.HighlighterFragmenter;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterPlainOptionsStep;
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterBuilder;
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterType;

public class HighlighterPlainOptionsStepImpl
		extends HighlighterOptionsStepImpl<HighlighterPlainOptionsStep>
		implements HighlighterPlainOptionsStep {
	public HighlighterPlainOptionsStepImpl(SearchHighlighterBuilder highlightBuilder) {
		super( highlightBuilder );
		this.highlighterBuilder.type( SearchHighlighterType.PLAIN );
	}

	@Override
	public HighlighterPlainOptionsStep fragmenter(HighlighterFragmenter type) {
		this.highlighterBuilder.fragmenter( type );
		return this;
	}
}
