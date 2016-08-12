/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.sort;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public interface SortMissingValueContext<T, U extends SortMissingValueContext.ValueTreatmentContext<?>> {

	// TODO YR Use the terms "highest" / "lowest" instead of last/first, which are dependent on the actual order (asc/desc)?

	/**
	 * Put missing values last in the sorting
	 */
	T sortLast();

	/**
	 * Put missing values first in the sorting
	 */
	T sortFirst();

	/**
	 * Value to replace a missing value with during sorting.
	 */
	U use(Object value);

	interface ValueTreatmentContext<T> {

		/**
		 * Do not try and find the field bridge nor apply the object / string conversion
		 * matching objects should be of type String in this case.
		 */
		T ignoreFieldBridge();
	}
}
