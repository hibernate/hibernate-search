/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.lowlevel.query.impl.Queries;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

public class PredicateRequestContext {

	private final BackendSessionContext sessionContext;
	private final String nestedPath;
	private JsonElement jsonKnn = JsonNull.INSTANCE;

	public PredicateRequestContext(BackendSessionContext sessionContext) {
		this.sessionContext = sessionContext;
		this.nestedPath = null;
	}

	private PredicateRequestContext(BackendSessionContext sessionContext, String nestedPath) {
		this.sessionContext = sessionContext;
		this.nestedPath = nestedPath;
	}

	String getTenantId() {
		return sessionContext.tenantIdentifier();
	}

	public PredicateRequestContext withNestedPath(String path) {
		return new PredicateRequestContext( sessionContext, path );
	}

	public String getNestedPath() {
		return nestedPath;
	}

	public void contributeKnnClause(JsonObject knn) {
		if ( jsonKnn.isJsonNull() ) {
			jsonKnn = knn;
		}
		else if ( jsonKnn.isJsonArray() ) {
			jsonKnn.getAsJsonArray().add( knn );
		}
		else {
			JsonArray array = new JsonArray();
			array.add( jsonKnn );
			array.add( knn );
			jsonKnn = array;
		}
	}

	public JsonElement knnSearch(JsonArray filters) {
		if ( jsonKnn.isJsonNull() ) {
			return jsonKnn;
		}
		return addFiltersToKnn( jsonKnn, filters );
	}

	private static JsonElement addFiltersToKnn(JsonElement jsonKnn, JsonArray filters) {
		if ( filters == null || filters.isEmpty() ) {
			return jsonKnn;
		}

		if ( jsonKnn.isJsonArray() ) {
			for ( JsonElement jsonElement : jsonKnn.getAsJsonArray() ) {
				addFiltersToKnn( jsonElement.getAsJsonObject(), filters );
			}
		}
		else {
			addFiltersToKnn( jsonKnn.getAsJsonObject(), filters );
		}
		return jsonKnn;
	}

	private static void addFiltersToKnn(JsonObject jsonKnn, JsonArray filters) {
		JsonObjectAccessor filterAccessor = JsonAccessor.root().property( "filter" ).asObject();
		if ( filters.size() == 1 && filterAccessor.get( jsonKnn ).isEmpty() ) {
			filterAccessor.set( jsonKnn, filters.get( 0 ).getAsJsonObject() );
		}
		else {
			filterAccessor.set(
					jsonKnn, Queries.boolFilter( filterAccessor.getOrCreate( jsonKnn, Queries::matchAll ), filters ) );
		}
	}
}
