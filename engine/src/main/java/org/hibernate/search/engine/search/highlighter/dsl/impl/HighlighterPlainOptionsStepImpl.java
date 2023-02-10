/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	public HighlighterPlainOptionsStep maxAnalyzedOffset(int max) {
		highlighterBuilder.maxAnalyzedOffset( max );
		return this;
	}

	@Override
	public HighlighterPlainOptionsStep fragmenter(HighlighterFragmenter type) {
		this.highlighterBuilder.fragmenter( type );
		return this;
	}
}
