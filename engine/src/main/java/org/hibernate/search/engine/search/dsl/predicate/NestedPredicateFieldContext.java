/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

/**
 * The context used when defining a "nested" predicate, after the object field was mentioned.
 *
 * @param <N> The type of the next context (returned after the nested query was defined).
 */
public interface NestedPredicateFieldContext<N> extends SearchPredicateContainerContext<N> {

	// TODO add tuning methods, like the "score_mode" in Elasticsearch (avg, min, ...)

}
