/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.definition;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A component able to define a predicate using the Hibernate Search Predicate DSL.
 * <p>
 * This definition takes advantage of provided metadata
 * to pick, configure and create a {@link SearchPredicate}.
 * <p>
 * Used in particular for named predicates,
 * where the definition is given a name and assigned to an element in the index schema.
 *
 * @see SearchPredicate
 *
 */
@Incubating
public interface PredicateDefinition {

	/**
	 * Creates a predicate.
	 * @param context The context, exposing in particular a {@link SearchPredicateFactory}.
	 * @return The created {@link SearchPredicate}.
	 */
	SearchPredicate create(PredicateDefinitionContext context);

}
