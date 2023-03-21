/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.highlighter.impl;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.highlighter.SearchHighlighter;
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterType;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

public interface ElasticsearchSearchHighlighter extends SearchHighlighter {

	Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	void request(JsonObject requestBody);

	void applyToField(String path, JsonObject fields);

	Set<String> indexNames();

	SearchHighlighterType type();

	static ElasticsearchSearchHighlighter from(ElasticsearchSearchIndexScope<?> scope, SearchHighlighter highlighter) {
		if ( !( highlighter instanceof ElasticsearchSearchHighlighter ) ) {
			throw log.cannotMixElasticsearchSearchQueryWithOtherQueryHighlighters( highlighter );
		}
		ElasticsearchSearchHighlighter casted = (ElasticsearchSearchHighlighter) highlighter;
		if ( !scope.hibernateSearchIndexNames().equals( casted.indexNames() ) ) {
			throw log.queryHighlighterDefinedOnDifferentIndexes( highlighter, casted.indexNames(),
					scope.hibernateSearchIndexNames()
			);
		}
		return casted;
	}
}
