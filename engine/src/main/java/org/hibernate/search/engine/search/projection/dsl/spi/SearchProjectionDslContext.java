/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.spi;

import org.hibernate.search.engine.search.projection.spi.SearchProjectionIndexScope;

/**
 * Represents the current context in the search DSL,
 * including in particular the search scope and the projection builder factory.
 *
 * @param <SC> The type of the backend-specific search scope.
 */
public final class SearchProjectionDslContext<SC extends SearchProjectionIndexScope<?>> {

	public static <SC extends SearchProjectionIndexScope<?>> SearchProjectionDslContext<SC> root(SC scope) {
		return new SearchProjectionDslContext<>( scope );
	}

	private final SC scope;

	private SearchProjectionDslContext(SC scope) {
		this.scope = scope;
	}

	/**
	 * @return The search scope. Will always return the exact same instance for a given DSL context.
	 */
	public SC scope() {
		return scope;
	}

}
