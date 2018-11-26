/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import com.google.gson.JsonPrimitive;

class ElasticsearchIndexOrderSortBuilder implements ElasticsearchSearchSortBuilder {

	public static final ElasticsearchIndexOrderSortBuilder INSTANCE = new ElasticsearchIndexOrderSortBuilder();

	private static final JsonPrimitive DOC_SORT_KEYWORD_JSON = new JsonPrimitive( "_doc" );

	private ElasticsearchIndexOrderSortBuilder() {
	}

	@Override
	public void buildAndAddTo(ElasticsearchSearchSortCollector collector) {
		collector.collectSort( DOC_SORT_KEYWORD_JSON );
	}
}
