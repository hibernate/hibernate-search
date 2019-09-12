/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.dsl;

import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

/**
 * Defines the storage strategy for an object field.
 */
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
	 *     <li>title = Levianthan Wakes</li>
	 *     <li>authors =
	 *         <ul>
	 *             <li>(first element)
	 *                 <ul>
	 *                     <li>firstName = Daniel</li>
	 *                     <li>lastName = Abraham</li>
	 *                 </ul>
	 *             </li>
	 *             <li>(second element)
	 *                 <ul>
	 *                     <li>firstName = Ty</li>
	 *                     <li>lastName = Frank</li>
	 *                 </ul>
	 *             </li>
	 *         </ul>
	 *     </li>
	 * </ul>
	 * Will be stored as:
	 * <ul>
	 *     <li>title = Levianthan Wakes</li>
	 *     <li>authors.firstName =
	 *         <ul>
	 *             <li>(first element) Daniel</li>
	 *             <li>(second element) Ty</li>
	 *         </ul>
	 *     </li>
	 *     <li>authors.lastName =
	 *         <ul>
	 *             <li>(first element) Abraham</li>
	 *             <li>(second element) Frank</li>
	 *         </ul>
	 *     </li>
	 * </ul>
	 *
	 * As a result, a search for <code>authors.firstname:Ty AND authors.lastname=Abraham</code>
	 * would return the above document even though Ty Abraham does not exist.
	 */
	FLATTENED,
	/**
	 * Store object fields as nested documents.
	 * <p>
	 * This storage mode is generally less efficient, but has the advantage of
	 * keeping structural information.
	 * Note however that access to that information when querying
	 * requires special care.
	 * See in particular the {@link SearchPredicateFactory#nested() "nested" predicate}.
	 */
	NESTED

}
