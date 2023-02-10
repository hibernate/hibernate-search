/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
