/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.reporting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contextual information about a failing index operation.
 */
public class IndexFailureContext extends FailureContext {

	public static Builder builder() {
		return new Builder();
	}

	private final List<Object> uncommittedOperations;

	private IndexFailureContext(Builder builder) {
		super( builder );
		this.uncommittedOperations = builder.uncommittedOperations == null
				? Collections.emptyList() : Collections.unmodifiableList( builder.uncommittedOperations );
	}

	/**
	 * @return The list of index operations that weren't committed yet when the failure occurred.
	 * Never {@code null}, but may be empty.
	 * These operations may not have been applied to the index.
	 * Use {@link Object#toString()} to get a textual representation of each operation.
	 */
	public List<Object> getUncommittedOperations() {
		return uncommittedOperations;
	}

	public static class Builder extends FailureContext.Builder {

		private List<Object> uncommittedOperations;

		private Builder() {
		}

		public void uncommittedOperation(Object uncommittedOperation) {
			if ( uncommittedOperations == null ) {
				uncommittedOperations = new ArrayList<>();
			}
			uncommittedOperations.add( uncommittedOperation );
		}

		@Override
		public IndexFailureContext build() {
			return new IndexFailureContext( this );
		}
	}
}
