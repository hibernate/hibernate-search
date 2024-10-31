/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.highlighter.impl;

import static org.hibernate.search.util.common.impl.CollectionHelper.isSubset;
import static org.hibernate.search.util.common.impl.CollectionHelper.notInTheOtherSet;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.logging.impl.QueryLog;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.highlighter.SearchHighlighter;
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterType;
import org.hibernate.search.engine.search.projection.ProjectionCollector;

import com.google.gson.JsonObject;

public interface ElasticsearchSearchHighlighter extends SearchHighlighter {

	void request(JsonObject requestBody);

	void applyToField(String path, JsonObject fields);

	Set<String> indexNames();

	SearchHighlighterType type();

	boolean isCompatible(ProjectionCollector.Provider<?, ?> collectorProvider);

	static ElasticsearchSearchHighlighter from(ElasticsearchSearchIndexScope<?> scope, SearchHighlighter highlighter) {
		if ( !( highlighter instanceof ElasticsearchSearchHighlighter ) ) {
			throw QueryLog.INSTANCE.cannotMixElasticsearchSearchQueryWithOtherQueryHighlighters( highlighter );
		}
		ElasticsearchSearchHighlighter casted = (ElasticsearchSearchHighlighter) highlighter;
		if ( !isSubset( scope.hibernateSearchIndexNames(), casted.indexNames() ) ) {
			throw QueryLog.INSTANCE.queryHighlighterDefinedOnDifferentIndexes( highlighter, casted.indexNames(),
					scope.hibernateSearchIndexNames(),
					notInTheOtherSet( scope.hibernateSearchIndexNames(), casted.indexNames() )
			);
		}
		return casted;
	}
}
