/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateCollector;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortCollector;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ElasticsearchSearchQueryElementCollector
		implements ElasticsearchSearchPredicateCollector, ElasticsearchSearchSortCollector {

	private JsonObject jsonPredicate;
	private JsonArray jsonSort;

	@Override
	public void collectPredicate(JsonObject jsonQuery) {
		this.jsonPredicate = jsonQuery;
	}

	@Override
	public void collectSort(JsonElement sort) {
		if ( jsonSort == null ) {
			jsonSort = new JsonArray();
		}
		this.jsonSort.add( sort );
	}

	@Override
	public void collectSort(JsonArray sorts) {
		if ( jsonSort == null ) {
			jsonSort = new JsonArray();
		}
		this.jsonSort.addAll( sorts );
	}

	public JsonObject toJsonPredicate() {
		return jsonPredicate;
	}

	public JsonArray toJsonSort() {
		return jsonSort;
	}

}
