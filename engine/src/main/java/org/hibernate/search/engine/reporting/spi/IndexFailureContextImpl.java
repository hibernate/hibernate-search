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

public class IndexFailureContextImpl extends FailureContextImpl
		implements IndexFailureContext {

	private final List<Object> uncommittedOperations;

	private IndexFailureContextImpl(Builder builder) {
		super( builder );
		this.uncommittedOperations = builder.uncommittedOperations == null
				? Collections.emptyList() : Collections.unmodifiableList( builder.uncommittedOperations );
	}

	@Override
	public List<Object> getUncommittedOperations() {
		return uncommittedOperations;
	}

	public static class Builder extends FailureContextImpl.Builder {

		private List<Object> uncommittedOperations;

		public void uncommittedOperation(Object uncommittedOperation) {
			if ( uncommittedOperations == null ) {
				uncommittedOperations = new ArrayList<>();
			}
			uncommittedOperations.add( uncommittedOperation );
		}

		@Override
		public IndexFailureContext build() {
			return new IndexFailureContextImpl( this );
		}
	}
}
