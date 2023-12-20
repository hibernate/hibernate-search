/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.lowlevel.query.impl.Queries;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

public class PredicateRequestContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PredicateRequestContext parent;
	private final Class<? extends ElasticsearchSearchPredicate> predicateType;
	private final BackendSessionContext sessionContext;
	private final String nestedPath;
	private JsonElement jsonKnn = JsonNull.INSTANCE;

	public PredicateRequestContext(BackendSessionContext sessionContext) {
		this.sessionContext = sessionContext;
		this.nestedPath = null;
		this.parent = null;
		this.predicateType = null;
	}

	private PredicateRequestContext(BackendSessionContext sessionContext, String nestedPath, PredicateRequestContext parent,
			Class<? extends ElasticsearchSearchPredicate> predicateType) {
		this.sessionContext = sessionContext;
		this.nestedPath = nestedPath;
		this.parent = parent;
		this.predicateType = predicateType;
	}

	String getTenantId() {
		return sessionContext.tenantIdentifier();
	}

	public PredicateRequestContext withNestedPath(String path) {
		return new PredicateRequestContext( sessionContext, path, this, predicateType );
	}

	public PredicateRequestContext predicateContext(ElasticsearchSearchPredicate predicate) {
		return new PredicateRequestContext( sessionContext, nestedPath, this, predicate.getClass() );
	}

	public String getNestedPath() {
		return nestedPath;
	}

	public void contributeKnnClause(JsonObject knn) {
		if ( !canAcceptKnnClause() ) {
			throw log.cannotAddKnnClauseAtThisStep();
		}
		if ( parent != null ) {
			parent.contributeKnnClause( knn );
		}
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

	private boolean canAcceptKnnClause() {
		// to allow knn being added to a root context:
		return parent == null
				// we are adding a knn predicate directly as a root predicate
				|| parent.isRoot()
				// we are adding a knn should clause to a root boolean predicate.
				// the fact that it is a should clause is checked in the bool predicate itself.
				|| parent.isBooleanPredicateContext() && parent.parent != null && parent.parent.isRoot();
	}

	private boolean isBooleanPredicateContext() {
		return ElasticsearchBooleanPredicate.class.equals( this.predicateType );
	}

	private boolean isRoot() {
		return parent == null;
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

	@Override
	public String toString() {
		return "PredicateRequestContext{" +
				"parent=" + parent +
				", predicateType=" + ( predicateType == null ? "root" : predicateType.getSimpleName() ) +
				", nestedPath='" + nestedPath + '\'' +
				'}';
	}
}
