/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateCollector;

import com.google.gson.JsonObject;

public class ElasticsearchSearchQueryElementCollector
		implements ElasticsearchSearchPredicateCollector {

	private JsonObject jsonPredicate;

	@Override
	public void collectPredicate(JsonObject jsonQuery) {
		this.jsonPredicate = jsonQuery;
	}

	public JsonObject toJsonPredicate() {
		return jsonPredicate;
	}

}
