/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;
import org.hibernate.search.backend.lucene.lowlevel.query.impl.Queries;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneSearchQueryIndexScope;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

public abstract class PredicateRequestContext {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );
	private final String nestedPath;

	private PredicateRequestContext(String nestedPath) {
		this.nestedPath = nestedPath;
	}

	public String getNestedPath() {
		return nestedPath;
	}

	public abstract Query appendTenantAndRoutingFilters(Query originalFilterQuery);

	public abstract PredicateRequestContext withNestedPath(String nestedPath);

	public static PredicateRequestContext withSession(LuceneSearchQueryIndexScope<?> scope,
			BackendSessionContext sessionContext, Set<String> routingKeys) {
		Contracts.assertNotNull( scope, "scope" );
		Contracts.assertNotNull( scope, "sessionContext" );
		return new FullPredicateRequestContext( null, scope, sessionContext, routingKeys );
	}

	public static PredicateRequestContext withoutSession() {
		return new LimitedPredicateRequestContext( null );
	}

	private static class LimitedPredicateRequestContext extends PredicateRequestContext {

		public LimitedPredicateRequestContext(String nestedPath) {
			super( nestedPath );
		}

		@Override
		public Query appendTenantAndRoutingFilters(Query originalFilterQuery) {
			// this context is created via migration utils, where the predicates are created as queries,
			//  hence we should not expect that a knn predicate is passed in.
			//  Alternatively it can be created in a place which we have total control over, and we only need to create an exists predicate,
			//  which does not need the session context anyway.
			throw new AssertionFailure( "A tenant/routing filter requires session context." );
		}

		@Override
		public PredicateRequestContext withNestedPath(String nestedPath) {
			return new LimitedPredicateRequestContext( nestedPath );
		}
	}

	private static class FullPredicateRequestContext extends PredicateRequestContext {
		private final LuceneSearchQueryIndexScope<?> scope;

		private final BackendSessionContext sessionContext;
		private final Set<String> routingKeys;

		private FullPredicateRequestContext(String nestedPath, LuceneSearchQueryIndexScope<?> scope,
				BackendSessionContext sessionContext, Set<String> routingKeys) {
			super( nestedPath );
			this.scope = scope;
			this.sessionContext = sessionContext;
			this.routingKeys = routingKeys;
		}

		public Query appendTenantAndRoutingFilters(Query originalFilterQuery) {
			// We append all these "filters" as must clauses since the constructed query will be passed in as a filter itself:
			BooleanQuery.Builder filterBuilder = new BooleanQuery.Builder();
			if ( originalFilterQuery != null ) {
				filterBuilder.add( originalFilterQuery, BooleanClause.Occur.MUST );
			}

			Query tenantFilter = scope.filterOrNull( sessionContext.tenantIdentifier() );
			if ( tenantFilter != null ) {
				filterBuilder.add( tenantFilter, BooleanClause.Occur.MUST );
			}

			if ( !routingKeys.isEmpty() ) {
				Query routingKeysQuery = Queries.anyTerm( MetadataFields.routingKeyFieldName(), routingKeys );
				filterBuilder.add( routingKeysQuery, BooleanClause.Occur.MUST );
			}
			BooleanQuery filter = filterBuilder.build();
			return filter.clauses().isEmpty() ? null : filter;
		}

		public PredicateRequestContext withNestedPath(String nestedPath) {
			return new FullPredicateRequestContext( nestedPath, scope, sessionContext, routingKeys );
		}
	}
}
