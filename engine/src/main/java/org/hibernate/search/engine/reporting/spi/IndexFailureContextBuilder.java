/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.reporting.spi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.reporting.IndexFailureContext;
import org.hibernate.search.engine.reporting.impl.IndexFailureContextImpl;

public class IndexFailureContextBuilder {

	private Throwable th;
	private Object failingOperation;
	private List<Object> uncommittedOperations;

	public IndexFailureContextBuilder throwable(Throwable th) {
		this.th = th;
		return this;
	}

	public IndexFailureContextBuilder failingOperation(Object failingOperation) {
		this.failingOperation = failingOperation;
		return this;
	}

	public IndexFailureContextBuilder uncommittedOperation(Object uncommittedOperation) {
		this.getUncommittedOperations().add( uncommittedOperation );
		return this;
	}

	public IndexFailureContext createFailureContext() {
		IndexFailureContextImpl context = new IndexFailureContextImpl();

		context.setThrowable( th );

		// for situation when there is a primary failure
		if ( failingOperation != null ) {
			context.setFailingOperation( failingOperation );
		}
		context.setUncommittedOperations( getUncommittedOperations() );
		return context;
	}

	private List<Object> getUncommittedOperations() {
		if ( uncommittedOperations == null ) {
			uncommittedOperations = new ArrayList<>();
		}
		return uncommittedOperations;
	}

}
