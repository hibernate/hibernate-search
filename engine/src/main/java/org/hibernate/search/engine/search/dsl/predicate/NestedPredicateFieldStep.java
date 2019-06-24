/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;


import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;

/**
 * The initial step in a "nested" predicate definition, where the target field can be set.
 */
public interface NestedPredicateFieldStep {

	/**
	 * Set the object field to "nest" on.
	 * <p>
	 * The selected field must be stored as {@link ObjectFieldStorage#NESTED} in the targeted indexes.
	 *
	 * @param absoluteFieldPath The path to the object.
	 * @return The next step.
	 */
	NestedPredicateNestStep onObjectField(String absoluteFieldPath);

}
