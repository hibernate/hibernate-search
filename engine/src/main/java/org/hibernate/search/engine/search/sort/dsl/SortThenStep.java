package org.hibernate.search.engine.search.sort.dsl;

/**
 * The step in a sort definition where another sort can be chained.
 *
 * @param <SR> Scope root type.
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public interface SortThenStep<SR> extends SortFinalStep {

	/**
	 * Start defining another sort, to be applied after the current one.
	 *
	 * @return The next step.
	 */
	TypedSearchSortFactory<SR> then();

}
