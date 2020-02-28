/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

import java.util.EnumSet;

/**
 * The step in a flags definition where the simple query can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step)
 */
public interface SimpleQueryFlagsStep<S extends SimpleQueryStringPredicateOptionsStep<?>> extends SimpleQueryStringPredicateOptionsStep<S> {

	/**
	 * Enable all operation in the given flags.
	 *
	 * @return {@code this}, for method chaining.
	 */
	default SimpleQueryFlagsStep<S> all() {
		EnumSet set = EnumSet.allOf( SimpleQueryFlag.class );
		enable( set );
		return this;
	}

	/**
	 * Enable operation in the given flags.
	 *
	 * @param operation The operation.
	 * @return {@code this}, for method chaining.
	 */
	SimpleQueryFlagsStep<S> enable(SimpleQueryFlag operation);

	/**
	 * Enable operation in the given flags.
	 *
	 * @param operations The operation.
	 * @return {@code this}, for method chaining.
	 */
	SimpleQueryFlagsStep<S> enable(EnumSet<SimpleQueryFlag> operations);

	/**
	 * Disable operation in the given flags.
	 *
	 * @param operation The operation.
	 * @return {@code this}, for method chaining.
	 */
	SimpleQueryFlagsStep<S> disable(SimpleQueryFlag operation);

	/**
	 * Disable operation in the given flags.
	 *
	 * @param operations The operation.
	 * @return {@code this}, for method chaining.
	 */
	SimpleQueryFlagsStep<S> disable(EnumSet<SimpleQueryFlag> operations);

}
