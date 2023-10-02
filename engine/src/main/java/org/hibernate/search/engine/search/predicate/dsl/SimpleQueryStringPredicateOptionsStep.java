/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * The final step in a "simple query string" predicate definition, where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 */
public interface SimpleQueryStringPredicateOptionsStep<S extends SimpleQueryStringPredicateOptionsStep<?>>
		extends CommonQueryStringPredicateOptionsStep<S> {

	/**
	 * Enable operation in the given flags.
	 *
	 * @param flags The operation flags.
	 * @return {@code this}, for method chaining.
	 */
	default S flags(SimpleQueryFlag... flags) {
		return flags( EnumSet.copyOf( Arrays.asList( flags ) ) );
	}

	/**
	 * Enable operation in the given flags.
	 *
	 * @param flags The operation flags.
	 * @return {@code this}, for method chaining.
	 */
	S flags(Set<SimpleQueryFlag> flags);

}
