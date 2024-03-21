/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.util.Optional;
import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;
import org.hibernate.search.backend.lucene.lowlevel.query.impl.Queries;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneSearchQueryIndexScope;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.engine.search.query.spi.QueryParameters;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Contracts;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

public abstract class PredicateRequestContext {
	private final String nestedPath;

	private PredicateRequestContext(String nestedPath) {
		this.nestedPath = nestedPath;
	}

	public String getNestedPath() {
		return nestedPath;
	}

	public abstract Query appendTenantAndRoutingFilters(Query originalFilterQuery);

	public abstract PredicateRequestContext withNestedPath(String nestedPath);

	public abstract NamedValues queryParameters();

	public static PredicateRequestContext withSession(LuceneSearchQueryIndexScope<?> scope,
			BackendSessionContext sessionContext, Set<String> routingKeys, QueryParameters parameters) {
		Contracts.assertNotNull( scope, "scope" );
		Contracts.assertNotNull( scope, "sessionContext" );
		return new FullPredicateRequestContext( null, scope, sessionContext, routingKeys, parameters );
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

		@Override
		public NamedValues queryParameters() {
			return FailingQueryParameters.INSTANCE;
		}

		private static class FailingQueryParameters implements NamedValues {
			private static final FailingQueryParameters INSTANCE = new FailingQueryParameters();

			@Override
			public <T> T get(String parameterName, Class<T> parameterValueType) {
				throw new AssertionFailure( "Accessing parameters requires session context." );
			}

			@Override
			public <T> Optional<T> getOptional(String parameterName, Class<T> parameterValueType) {
				throw new AssertionFailure( "Accessing parameters requires session context." );
			}
		}
	}

	private static class FullPredicateRequestContext extends PredicateRequestContext {
		private final LuceneSearchQueryIndexScope<?> scope;

		private final BackendSessionContext sessionContext;
		private final Set<String> routingKeys;
		private final QueryParameters parameters;

		private FullPredicateRequestContext(String nestedPath, LuceneSearchQueryIndexScope<?> scope,
				BackendSessionContext sessionContext, Set<String> routingKeys, QueryParameters parameters) {
			super( nestedPath );
			this.scope = scope;
			this.sessionContext = sessionContext;
			this.routingKeys = routingKeys;
			this.parameters = parameters;
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
			return new FullPredicateRequestContext( nestedPath, scope, sessionContext, routingKeys, parameters );
		}

		@Override
		public NamedValues queryParameters() {
			return parameters;
		}
	}
}
