/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

import org.hibernate.search.engine.search.dsl.ExplicitEndContext;

/**
 * The context used when at least one "minimum should match" constraint was defined,
 * allowing to {@link #ifMoreThan(int) define more constraints}
 * or to {@link #end() end} the definition and get back to the parent context.
 * <p>
 * See <a href="MinimumShouldMatchContext.html#minimumshouldmatch">"minimumShouldMatch" constraints</a>.
 *
 * @param <N> The type of the next context (returned by {@link MinimumShouldMatchNonEmptyContext#end()}).
 */
public interface MinimumShouldMatchNonEmptyContext<N> extends MinimumShouldMatchContext<N>, ExplicitEndContext<N> {

}
