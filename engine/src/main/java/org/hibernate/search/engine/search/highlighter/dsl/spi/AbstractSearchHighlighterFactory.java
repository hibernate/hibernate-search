/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.highlighter.dsl.spi;

import org.hibernate.search.engine.search.common.spi.SearchIndexScope;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterFastVectorHighlighterOptionsStep;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterPlainOptionsStep;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterUnifiedOptionsStep;
import org.hibernate.search.engine.search.highlighter.dsl.SearchHighlighterFactory;
import org.hibernate.search.engine.search.highlighter.dsl.impl.HighlighterFastVectorHighlighterOptionsStepImpl;
import org.hibernate.search.engine.search.highlighter.dsl.impl.HighlighterPlainOptionsStepImpl;
import org.hibernate.search.engine.search.highlighter.dsl.impl.HighlighterUnifiedOptionsStepImpl;
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterBuilder;

public abstract class AbstractSearchHighlighterFactory<SC extends SearchIndexScope<?>>
		implements SearchHighlighterFactory {

	private final SC scope;

	protected AbstractSearchHighlighterFactory(SC scope) {
		this.scope = scope;
	}

	@Override
	public HighlighterUnifiedOptionsStep unified() {
		return new HighlighterUnifiedOptionsStepImpl(
				highlighterBuilder( scope )
		);
	}

	@Override
	public HighlighterPlainOptionsStep plain() {
		return new HighlighterPlainOptionsStepImpl(
				highlighterBuilder( scope )
		);
	}

	@Override
	public HighlighterFastVectorHighlighterOptionsStep fastVector() {
		return new HighlighterFastVectorHighlighterOptionsStepImpl(
				highlighterBuilder( scope )
		);
	}

	protected abstract SearchHighlighterBuilder highlighterBuilder(SC scope);

}
