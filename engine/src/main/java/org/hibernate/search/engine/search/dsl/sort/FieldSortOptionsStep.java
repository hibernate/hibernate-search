/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort;

/**
 * The initial and final step in a "field" sort definition, where optional parameters can be set.
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public interface FieldSortOptionsStep
		extends SortFinalStep, SortThenStep, SortOrderStep<FieldSortOptionsStep> {

	/**
	 * Start describing the behavior of this sort when a document doesn't have any value for the targeted field.
	 *
	 * @return The next step.
	 */
	FieldSortMissingValueBehaviorStep<FieldSortOptionsStep> onMissingValue();

}
