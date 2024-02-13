/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;

public class PredicateRequestContext {

	private final BackendSessionContext sessionContext;
	private final ElasticsearchSearchIndexScope<?> searchIndexScope;
	private final String nestedPath;

	public PredicateRequestContext(BackendSessionContext sessionContext, ElasticsearchSearchIndexScope<?> searchIndexScope) {
		this( sessionContext, searchIndexScope, null );
	}

	private PredicateRequestContext(BackendSessionContext sessionContext, ElasticsearchSearchIndexScope<?> searchIndexScope,
			String nestedPath) {
		this.sessionContext = sessionContext;
		this.searchIndexScope = searchIndexScope;
		this.nestedPath = nestedPath;
	}

	public String getNestedPath() {
		return nestedPath;
	}

	String getTenantId() {
		return sessionContext.tenantIdentifier();
	}

	public ElasticsearchSearchIndexScope<?> getSearchIndexScope() {
		return searchIndexScope;
	}

	public PredicateRequestContext withNestedPath(String path) {
		return new PredicateRequestContext( sessionContext, searchIndexScope, path );
	}
}
