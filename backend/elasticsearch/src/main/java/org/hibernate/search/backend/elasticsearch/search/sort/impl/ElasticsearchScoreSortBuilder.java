/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import org.hibernate.search.engine.search.sort.spi.ScoreSortBuilder;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

class ElasticsearchScoreSortBuilder extends AbstractElasticsearchSearchSortBuilder
		implements ScoreSortBuilder<ElasticsearchSearchSortBuilder> {

	private static final String SCORE_SORT_KEYWORD = "_score";
	private static final JsonPrimitive SCORE_SORT_KEYWORD_JSON = new JsonPrimitive( SCORE_SORT_KEYWORD );

	@Override
	public void doBuildAndAddTo(ElasticsearchSearchSortCollector collector, JsonObject innerObject) {
		if ( innerObject.size() == 0 ) {
			collector.collectSort( SCORE_SORT_KEYWORD_JSON );
		}
		else {
			JsonObject outerObject = new JsonObject();
			outerObject.add( SCORE_SORT_KEYWORD, innerObject );
			collector.collectSort( outerObject );
		}
	}
}
