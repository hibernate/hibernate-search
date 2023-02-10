/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.highlighter.dsl.impl;

import org.hibernate.search.engine.search.highlighter.SearchHighlighter;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterEncoder;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterFinalStep;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterOptionsStep;
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterBuilder;

public class HighlighterOptionsStepImpl<T extends HighlighterOptionsStep<T>>
		implements HighlighterOptionsStep<T>, HighlighterFinalStep {

	protected final SearchHighlighterBuilder highlighterBuilder;

	public HighlighterOptionsStepImpl(SearchHighlighterBuilder highlighterBuilder) {
		this.highlighterBuilder = highlighterBuilder;
	}

	@Override
	public T encoder(HighlighterEncoder encoder) {
		highlighterBuilder.encoder( encoder );
		return thisAsT();
	}

	@Override
	public T fragmentSize(int size) {
		highlighterBuilder.fragmentSize( size );
		return thisAsT();
	}

	@Override
	public T noMatchSize(int size) {
		highlighterBuilder.noMatchSize( size );
		return thisAsT();
	}

	@Override
	public T numberOfFragments(int number) {
		highlighterBuilder.numberOfFragments( number );
		return thisAsT();
	}

	@Override
	public T orderByScore(boolean enable) {
		highlighterBuilder.orderByScore( enable );
		return thisAsT();
	}

	@Override
	public T tag(String preTag, String postTag) {
		highlighterBuilder.clearTags();
		highlighterBuilder.tag( preTag, postTag );
		return thisAsT();
	}

	@SuppressWarnings("unchecked")
	private T thisAsT() {
		return (T) this;
	}


	@Override
	public SearchHighlighter toHighlighter() {
		return highlighterBuilder.build();
	}

}
