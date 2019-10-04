/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.reporting;

/**
 * Contextual information about a failing background operation.
 */
public interface FailureContext {

	/**
	 * @return The {@link Exception} or {@link Error} thrown when the operation failed.
	 * Never {@code null}.
	 */
	Throwable getThrowable();

	/**
	 * @return The operation that triggered the failure.
	 * Never {@code null}.
	 * Use {@link Object#toString()} to get a textual representation.
	 */
	Object getFailingOperation();

}
