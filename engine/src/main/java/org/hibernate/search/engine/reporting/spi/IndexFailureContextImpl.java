/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.reporting.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.search.engine.reporting.IndexFailureContext;

public class IndexFailureContextImpl implements IndexFailureContext {

	private final Throwable throwable;

	private final Object failingOperation;

	private final List<Object> uncommittedOperations;

	private IndexFailureContextImpl(Builder builder) {
		this.throwable = builder.throwable;
		this.failingOperation = builder.failingOperation;
		this.uncommittedOperations = builder.uncommittedOperations == null
				? Collections.emptyList() : Collections.unmodifiableList( builder.uncommittedOperations );
	}

	@Override
	public Throwable getThrowable() {
		return this.throwable;
	}

	@Override
	public Object getFailingOperation() {
		return this.failingOperation;
	}

	@Override
	public List<Object> getUncommittedOperations() {
		return uncommittedOperations;
	}

	public static class Builder {

		private Throwable throwable;
		private Object failingOperation;
		private List<Object> uncommittedOperations;

		public void throwable(Throwable th) {
			this.throwable = th;
		}

		public void failingOperation(Object failingOperation) {
			this.failingOperation = failingOperation;
		}

		public void uncommittedOperation(Object uncommittedOperation) {
			if ( uncommittedOperations == null ) {
				uncommittedOperations = new ArrayList<>();
			}
			uncommittedOperations.add( uncommittedOperation );
		}

		public IndexFailureContext build() {
			return new IndexFailureContextImpl( this );
		}
	}
}
