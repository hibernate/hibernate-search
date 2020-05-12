/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.session;

import java.util.Collection;
import java.util.Collections;

import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.mapper.javabean.common.EntityReference;
import org.hibernate.search.mapper.javabean.scope.SearchScope;
import org.hibernate.search.mapper.javabean.work.SearchIndexer;
import org.hibernate.search.mapper.javabean.work.SearchIndexingPlan;

public interface SearchSession extends AutoCloseable {

	/**
	 * Execute any pending work in the {@link #indexingPlan() indexing plan}
	 * and release any resource held by this session.
	 */
	@Override
	void close();

	/**
	 * Initiate the building of a search query.
	 * <p>
	 * The query will target the indexes mapped to the given type, or to any of its sub-types.
	 *
	 * @param type An indexed type, or a supertype of all indexed types that will be targeted by the search query.
	 * @return The initial step of a DSL where the search query can be defined.
	 * @see SearchQuerySelectStep
	 */
	default SearchQuerySelectStep<?, EntityReference, ?, ?, ?, ?> search(Class<?> type) {
		return search( Collections.singleton( type ) );
	}

	/**
	 * Initiate the building of a search query.
	 * <p>
	 * The query will target the indexes mapped to the given types, or to any of their sub-types.
	 *
	 * @param types A collection of indexed types, or supertypes of all indexed types that will be targeted by the search query.
	 * @return The initial step of a DSL where the search query can be defined.
	 * @see SearchQuerySelectStep
	 */
	SearchQuerySelectStep<?, EntityReference, ?, ?, ?, ?> search(Collection<? extends Class<?>> types);

	/**
	 * Initiate the building of a search query.
	 * <p>
	 * The query will target the indexes in the given scope.
	 *
	 * @param scope A scope representing all indexed types that will be targeted by the search query.
	 * @return The initial step of a DSL where the search query can be defined.
	 * @see SearchQuerySelectStep
	 */
	SearchQuerySelectStep<?, EntityReference, ?, ?, ?, ?> search(SearchScope scope);

	/**
	 * Create a {@link SearchScope} limited to the given type.
	 *
	 * @param type A type to include in the scope.
	 * @return The created scope.
	 * @see SearchScope
	 */
	default SearchScope scope(Class<?> type) {
		return scope( Collections.singleton( type ) );
	}

	/**
	 * Create a {@link SearchScope} limited to the given types.
	 *
	 * @param types A collection of types to include in the scope.
	 * @return The created scope.
	 * @see SearchScope
	 */
	SearchScope scope(Collection<? extends Class<?>> types);

	/**
	 * @return The indexing plan for this session. It will be executed upon closing this session.
	 */
	SearchIndexingPlan indexingPlan();

	/**
	 * @return The indexer for this session.
	 */
	SearchIndexer indexer();

}
