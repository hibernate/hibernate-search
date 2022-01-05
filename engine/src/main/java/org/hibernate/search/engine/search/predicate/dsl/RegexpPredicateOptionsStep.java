/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * The final step in a "regexp" predicate definition, where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 */
public interface RegexpPredicateOptionsStep<S extends RegexpPredicateOptionsStep<?>>
		extends PredicateFinalStep, PredicateScoreStep<S> {

	/**
	 * Enable operation in the given flags.
	 *
	 * @param flags The operation flags.
	 * @return {@code this}, for method chaining.
	 */
	default S flags(RegexpQueryFlag... flags) {
		return flags( EnumSet.copyOf( Arrays.asList( flags ) ) );
	}

	/**
	 * Enable operation in the given flags.
	 *
	 * @param flags The operation flags.
	 * @return {@code this}, for method chaining.
	 */
	S flags(Set<RegexpQueryFlag> flags);

}
