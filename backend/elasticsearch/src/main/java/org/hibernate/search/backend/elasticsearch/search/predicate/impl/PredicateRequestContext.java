/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.lowlevel.query.impl.Queries;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.engine.search.query.spi.QueryParameters;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class PredicateRequestContext {

	private final BackendSessionContext sessionContext;
	private final ElasticsearchSearchIndexScope<?> searchIndexScope;
	private final Set<String> routingKeys;
	private final QueryParameters parameters;
	private final String nestedPath;


	public PredicateRequestContext(BackendSessionContext sessionContext, ElasticsearchSearchIndexScope<?> searchIndexScope,
			Set<String> routingKeys, QueryParameters parameters) {
		this( sessionContext, searchIndexScope, routingKeys, parameters, null );
	}

	private PredicateRequestContext(BackendSessionContext sessionContext,
			ElasticsearchSearchIndexScope<?> searchIndexScope,
			Set<String> routingKeys, QueryParameters parameters, String nestedPath) {
		this.sessionContext = sessionContext;
		this.searchIndexScope = searchIndexScope;
		this.routingKeys = routingKeys;
		this.parameters = parameters;
		this.nestedPath = nestedPath;
	}

	public String getNestedPath() {
		return nestedPath;
	}

	String getTenantId() {
		return sessionContext.tenantIdentifier();
	}

	public JsonArray tenantAndRoutingFilters() {
		JsonArray filters = new JsonArray();
		JsonObject filter = searchIndexScope.filterOrNull( sessionContext.tenantIdentifier() );
		if ( filter != null ) {
			filters.add( filter );
		}
		if ( !routingKeys.isEmpty() ) {
			filters.add( Queries.anyTerm( "_routing", routingKeys ) );
		}
		return filters;
	}

	public PredicateRequestContext withNestedPath(String path) {
		return new PredicateRequestContext( sessionContext, searchIndexScope, routingKeys, parameters, path );
	}

	public NamedValues queryParameters() {
		return parameters;
	}
}
