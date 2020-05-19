/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.reporting;

import org.hibernate.search.util.common.AssertionFailure;

/**
 * Contextual information about a failing background operation.
 */
public class FailureContext {

	/**
	 * @return A new {@link FailureContext} builder.
	 */
	public static Builder builder() {
		return new Builder();
	}

	private final Throwable throwable;

	private final Object failingOperation;

	FailureContext(Builder builder) {
		/*
		 * Avoid nulls: they should not happen, and they are most likely bugs in Hibernate Search,
		 * but we don't want user-implemented failure handlers to fail because of that
		 * (they would throw an NPE which may produce disastrous results such as killing background threads).
		 */
		this.throwable = builder.throwable == null
				? new AssertionFailure(
				"Unknown throwable: missing throwable when reporting the failure."
						+ " There is probably a bug in Hibernate Search, please report it."
		)
				: builder.throwable;
		this.failingOperation = builder.failingOperation == null
				? "Unknown operation: missing operation when reporting the failure."
				+ " There is probably a bug in Hibernate Search, please report it."
				: builder.failingOperation;
	}

	/**
	 * @return The {@link Exception} or {@link Error} thrown when the operation failed.
	 * Never {@code null}.
	 */
	public Throwable throwable() {
		return throwable;
	}

	/**
	 * @return The {@link Exception} or {@link Error} thrown when the operation failed.
	 * Never {@code null}.
	 * @deprecated Use {@link #throwable} instead.
	 */
	@Deprecated
	public Throwable getThrowable() {
		return throwable();
	}

	/**
	 * @return The operation that triggered the failure.
	 * Never {@code null}.
	 * Use {@link Object#toString()} to get a textual representation.
	 */
	public Object failingOperation() {
		return this.failingOperation;
	}

	/**
	 * @return The operation that triggered the failure.
	 * Never {@code null}.
	 * Use {@link Object#toString()} to get a textual representation.
	 * @deprecated Use {@link #failingOperation()} instead.
	 */
	@Deprecated
	public Object getFailingOperation() {
		return this.failingOperation;
	}

	public static class Builder {

		private Throwable throwable;
		private Object failingOperation;

		Builder() {
		}

		public void throwable(Throwable th) {
			this.throwable = th;
		}

		public void failingOperation(Object failingOperation) {
			this.failingOperation = failingOperation;
		}

		public FailureContext build() {
			return new FailureContext( this );
		}
	}

}
