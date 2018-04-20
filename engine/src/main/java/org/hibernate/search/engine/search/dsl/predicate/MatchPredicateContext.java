/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;


/**
 * The context used when starting to define a match predicate.
 *
 * @param <N> The type of the next context (returned by {@link MatchPredicateFieldSetContext#matching(Object)}).
 */
public interface MatchPredicateContext<N> {

	// TODO wildcard, fuzzy. Or maybe a separate context?

	default MatchPredicateFieldSetContext<N> onField(String absoluteFieldPath) {
		return onFields( absoluteFieldPath );
	}

	MatchPredicateFieldSetContext<N> onFields(String ... absoluteFieldPaths);

}
