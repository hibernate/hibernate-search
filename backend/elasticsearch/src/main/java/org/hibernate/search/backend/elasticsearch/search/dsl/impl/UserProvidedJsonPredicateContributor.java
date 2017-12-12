/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.impl;

import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
class UserProvidedJsonPredicateContributor implements ElasticsearchSearchPredicateContributor {

	private static final Gson GSON = new GsonBuilder().create();

	private final JsonObject json;

	public UserProvidedJsonPredicateContributor(String jsonString) {
		this.json = GSON.fromJson( jsonString, JsonObject.class );
	}

	@Override
	public void contribute(Consumer<JsonObject> collector) {
		collector.accept( json );
	}

}
