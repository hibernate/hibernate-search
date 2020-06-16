/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;


import org.hibernate.search.engine.backend.types.ObjectStructure;

/**
 * The initial step in a "nested" predicate definition, where the target field can be set.
 *
 * @param <N> The type of the next step.
 */
public interface NestedPredicateFieldStep<N extends NestedPredicateNestStep<?>> {

	/**
	 * Set the object field to "nest" on.
	 * <p>
	 * The selected field must have a {@link ObjectStructure#NESTED nested structure} in the targeted indexes.
	 *
	 * @param absoluteFieldPath The path to the object.
	 * @return The next step.
	 */
	N objectField(String absoluteFieldPath);

}
