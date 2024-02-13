/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneSearchQueryIndexScope;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

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

	public abstract Query tenantFilterOrNull();

	public abstract PredicateRequestContext withNestedPath(String nestedPath);

	public static PredicateRequestContext withSession(LuceneSearchQueryIndexScope<?> scope,
			BackendSessionContext sessionContext) {
		Contracts.assertNotNull( scope, "scope" );
		Contracts.assertNotNull( scope, "sessionContext" );
		return new FullPredicateRequestContext( null, scope, sessionContext );
	}

	public static PredicateRequestContext withoutSession() {
		return new LimitedPredicateRequestContext( null );
	}

	private static class LimitedPredicateRequestContext extends PredicateRequestContext {

		public LimitedPredicateRequestContext(String nestedPath) {
			super( nestedPath );
		}

		@Override
		public Query tenantFilterOrNull() {
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

		private FullPredicateRequestContext(String nestedPath, LuceneSearchQueryIndexScope<?> scope,
				BackendSessionContext sessionContext) {
			super( nestedPath );
			this.scope = scope;
			this.sessionContext = sessionContext;
		}

		public Query tenantFilterOrNull() {
			String tenantIdentifier = sessionContext.tenantIdentifier();

			return tenantIdentifier == null ? null : scope.filterOrNull( tenantIdentifier );
		}

		public PredicateRequestContext withNestedPath(String nestedPath) {
			return new FullPredicateRequestContext( nestedPath, scope, sessionContext );
		}
	}
}
