/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.lowlevel.query.impl.Queries;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class PredicateRequestContext {

	private final BackendSessionContext sessionContext;
	private final ElasticsearchSearchIndexScope<?> searchIndexScope;
	private final Set<String> routingKeys;
	private final String nestedPath;


	public PredicateRequestContext(BackendSessionContext sessionContext, ElasticsearchSearchIndexScope<?> searchIndexScope,
			Set<String> routingKeys) {
		this( sessionContext, searchIndexScope, routingKeys, null );
	}

	private PredicateRequestContext(BackendSessionContext sessionContext,
			ElasticsearchSearchIndexScope<?> searchIndexScope,
			Set<String> routingKeys, String nestedPath) {
		this.sessionContext = sessionContext;
		this.searchIndexScope = searchIndexScope;
		this.routingKeys = routingKeys;
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
		return new PredicateRequestContext( sessionContext, searchIndexScope, routingKeys, path );
	}
}
