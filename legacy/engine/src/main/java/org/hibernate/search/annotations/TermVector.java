/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.annotations;

/**
 * Defines the term vector storing strategy
 *
 * @author John Griffin
 */
public enum TermVector {
	/**
	 * Store term vectors.
	 */
	YES,
	/**
	 * Do not store term vectors.
	 */
	NO,
	/**
	 * Store the term vector + Token offset information
	 */
	WITH_OFFSETS,
	/**
	 * Store the term vector + token position information
	 */
	WITH_POSITIONS,
	/**
	 * Store the term vector + Token position and offset information
	 */
	WITH_POSITION_OFFSETS
}
