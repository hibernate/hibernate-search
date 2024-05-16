/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl;

/**
 * The initial and final step in a "score" sort definition, where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public interface ScoreSortOptionsStep<E, S extends ScoreSortOptionsStep<E, ?>>
		extends SortFinalStep, SortThenStep<E>, SortOrderStep<S> {
}
