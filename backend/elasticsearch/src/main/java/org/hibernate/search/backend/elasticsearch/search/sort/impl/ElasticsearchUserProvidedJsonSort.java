/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;

import com.google.gson.JsonObject;

class ElasticsearchUserProvidedJsonSort extends AbstractElasticsearchSort {

	private final JsonObject json;

	ElasticsearchUserProvidedJsonSort(ElasticsearchSearchIndexScope<?> scope, JsonObject json) {
		super( scope );
		this.json = json;
	}

	@Override
	public void toJsonSorts(ElasticsearchSearchSortCollector collector) {
		collector.collectSort( json );
	}

}
