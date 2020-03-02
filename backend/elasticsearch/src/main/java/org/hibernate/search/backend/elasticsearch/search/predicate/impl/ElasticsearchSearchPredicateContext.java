/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;

public class ElasticsearchSearchPredicateContext {

	private final BackendSessionContext sessionContext;
	private final Set<String> explicitNestedPaths;

	public ElasticsearchSearchPredicateContext(BackendSessionContext sessionContext) {
		this.sessionContext = sessionContext;
		this.explicitNestedPaths = Collections.emptySet();
	}

	private ElasticsearchSearchPredicateContext(BackendSessionContext sessionContext, Set<String> explicitNestedPaths) {
		this.sessionContext = sessionContext;
		this.explicitNestedPaths = Collections.unmodifiableSet( explicitNestedPaths );
	}

	String getTenantId() {
		return sessionContext.getTenantIdentifier();
	}

	public ElasticsearchSearchPredicateContext explicitNested(String path) {
		HashSet<String> paths = new HashSet<>( explicitNestedPaths );
		paths.add( path );
		return new ElasticsearchSearchPredicateContext( sessionContext, paths );
	}

	public boolean isExplicitNested(String path) {
		// Checking explicitNestedPaths#contins( path ) could be not enough
		// for cases where explicit nest is used only for some nested level of the hierarchy:
		// see NestedSearchPredicateIT#search_nestedOnTwoLevels_onlySecondLevel.
		for ( String explicitNestedPath : explicitNestedPaths ) {
			if ( explicitNestedPath.startsWith( path ) ) {
				return true;
			}
		}
		return false;
	}
}
