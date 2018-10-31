/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.dsl;

import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;

public enum ObjectFieldStorage {

	/**
	 * Use the backend-specific default.
	 */
	DEFAULT,
	/**
	 * Flatten multi-valued object fields.
	 * <p>
	 * This storage mode is generally more efficient, but has the disadvantage of
	 * losing structural information by making the leaf fields multi-valued instead of the object fields.
	 * <p>
	 * For instance this structure:
	 * <ul>
	 *     <li>person =
	 *     <ul>
	 *         <li>(first element)
	 *         <ul>
	 *             <li>firstname = john</li>
	 *             <li>lastname = doe</li>
	 *         </ul>
	 *         <li>(second element)
	 *         <ul>
	 *             <li>firstname = harold</li>
	 *             <li>lastname = smith</li>
	 *         </ul>
	 *     </ul>
	 *     </li>
	 * </ul>
	 * Will be stored as:
	 * <ul>
	 *     <li>person.firstname = john, jane</li>
	 *     <li>person.lastname = doe, smith</li>
	 * </ul>
	 *
	 * As a result, a search for <code>person.firstname:john AND person.lastname=smith</code>
	 * would return the above document even though John Smith wasn't referenced in the document.
	 */
	FLATTENED,
	/**
	 * Store object fields as nested documents.
	 * <p>
	 * This storage mode is generally less efficient, but has the advantage of
	 * keeping structural information, allowing the use of
	 * {@link SearchPredicateFactoryContext#nested() "nested" predicates}.
	 */
	NESTED

}
