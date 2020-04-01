/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.factories;

import org.hibernate.search.engine.search.predicate.SearchPredicate;

/**
 * A filter from a index scope.
 * <p>
 * This factory takes advantage of provided metadata
 * to pick, configure and create a {@link SearchPredicate}.
 *
 * @see SearchPredicate
 */
public interface FilterFactory {

	/**
	 * Creating filter search predicate.
	 * @param ctx the context {@link SearchPredicate}.
	 * @return The {@link SearchPredicate} resulting from the previous DSL steps.
	 */
	SearchPredicate create(FilterFactoryContext ctx);

}
