/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.session;

import java.util.Collection;
import java.util.Collections;

import org.hibernate.search.engine.search.dsl.query.SearchQueryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.mapper.javabean.search.SearchScope;
import org.hibernate.search.mapper.javabean.work.SearchWorkPlan;
import org.hibernate.search.mapper.pojo.search.PojoReference;

public interface SearchSession extends AutoCloseable {

	/**
	 * Execute any pending work in the {@link #getMainWorkPlan() main work plan}
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
	 * @return A context allowing to define the search query,
	 * and ultimately {@link SearchQueryContext#toQuery() get the resulting query}.
	 * @see SearchQueryResultDefinitionContext
	 */
	default SearchQueryResultDefinitionContext<PojoReference, ?, ?> search(Class<?> type) {
		return scope( type ).search();
	}

	/**
	 * Initiate the building of a search query.
	 * <p>
	 * The query will target the indexes mapped to the given types, or to any of their sub-types.
	 *
	 * @param types A collection of indexed types, or supertypes of all indexed types that will be targeted by the search query.
	 * @return A context allowing to define the search query,
	 * and ultimately {@link SearchQueryContext#toQuery() get the resulting query}.
	 * @see SearchQueryResultDefinitionContext
	 */
	default SearchQueryResultDefinitionContext<PojoReference, ?, ?> search(Collection<? extends Class<?>> types) {
		return scope( types ).search();
	}

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
	 * @return The main work plan for this session. It will be executed upon closing this session.
	 */
	SearchWorkPlan getMainWorkPlan();

}
