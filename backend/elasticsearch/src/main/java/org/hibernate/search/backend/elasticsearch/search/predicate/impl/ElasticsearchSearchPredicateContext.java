/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;

public class ElasticsearchSearchPredicateContext {

	private final BackendSessionContext sessionContext;
	private final String nestedPath;

	public ElasticsearchSearchPredicateContext(BackendSessionContext sessionContext) {
		this.sessionContext = sessionContext;
		this.nestedPath = null;
	}

	private ElasticsearchSearchPredicateContext(BackendSessionContext sessionContext, String nestedPath) {
		this.sessionContext = sessionContext;
		this.nestedPath = nestedPath;
	}

	String getTenantId() {
		return sessionContext.getTenantIdentifier();
	}

	public ElasticsearchSearchPredicateContext explicitNested(String path) {
		return new ElasticsearchSearchPredicateContext( sessionContext, path );
	}

	public String getNestedPath() {
		return nestedPath;
	}
}
