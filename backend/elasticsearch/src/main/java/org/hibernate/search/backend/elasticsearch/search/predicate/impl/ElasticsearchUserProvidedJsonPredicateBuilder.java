/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import com.google.gson.JsonObject;


class ElasticsearchUserProvidedJsonPredicateBuilder implements ElasticsearchSearchPredicateBuilder {

	private final JsonObject json;

	ElasticsearchUserProvidedJsonPredicateBuilder(JsonObject json) {
		this.json = json;
	}

	@Override
	public void checkNestableWithin(String expectedParentNestedPath) {
		// Nothing to do: we'll assume the user knows what they are doing.
	}

	@Override
	public JsonObject build(
			ElasticsearchSearchPredicateContext context) {
		return json;
	}

}
