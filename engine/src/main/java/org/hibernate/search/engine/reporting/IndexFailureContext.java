/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.reporting;

import java.util.List;

/**
 * Contextual information about a failing index operation.
 */
public interface IndexFailureContext extends FailureContext {

	/**
	 * @return The list of index operations that weren't committed yet when the failure occurred.
	 * These operations may not have been applied to the index.
	 * Use {@link Object#toString()} to get a textual representation of each operation.
	 */
	List<Object> getUncommittedOperations();

}
