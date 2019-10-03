/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.reporting.spi;

import org.hibernate.search.engine.reporting.FailureContext;

public class FailureContextImpl implements FailureContext {

	private final Throwable throwable;

	private final Object failingOperation;

	FailureContextImpl(Builder builder) {
		this.throwable = builder.throwable;
		this.failingOperation = builder.failingOperation;
	}

	@Override
	public Throwable getThrowable() {
		return this.throwable;
	}

	@Override
	public Object getFailingOperation() {
		return this.failingOperation;
	}

	public static class Builder {

		private Throwable throwable;
		private Object failingOperation;

		public void throwable(Throwable th) {
			this.throwable = th;
		}

		public void failingOperation(Object failingOperation) {
			this.failingOperation = failingOperation;
		}

		public FailureContext build() {
			return new FailureContextImpl( this );
		}
	}
}
