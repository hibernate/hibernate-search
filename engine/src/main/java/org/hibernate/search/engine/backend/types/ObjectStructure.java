/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.types;

import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

/**
 * Defines how the structure of an object field is preserved upon indexing.
 */
public enum ObjectStructure {

	/**
	 * Use the backend-specific default.
	 */
	DEFAULT,
	/**
	 * Flatten multi-valued object fields.
	 * <p>
	 * This structure is generally more efficient,
	 * but has the disadvantage of dropping the original structure
	 * by making the leaf fields multi-valued instead of the object fields.
	 * <p>
	 * For instance this structure:
	 * <ul>
	 *     <li>title = Leviathan Wakes</li>
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
	 * Will become:
	 * <ul>
	 *     <li>title = Leviathan Wakes</li>
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
	 * This structure is generally less efficient,
	 * but has the advantage of preserving the original structure.
	 * Note however that access to that information when querying
	 * requires special care.
	 * See in particular the {@link SearchPredicateFactory#nested(String) "nested" predicate}.
	 */
	NESTED

}
