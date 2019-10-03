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
	private Object operationAtFault;
	private List<Object> failingOperations;

	public IndexFailureContextBuilder throwable(Throwable th) {
		this.th = th;
		return this;
	}

	public IndexFailureContextBuilder operationAtFault(Object operationAtFault) {
		this.operationAtFault = operationAtFault;
		return this;
	}

	public IndexFailureContextBuilder addWorkThatFailed(Object failedWork) {
		this.getFailingOperations().add( failedWork );
		return this;
	}

	public IndexFailureContext createFailureContext() {
		IndexFailureContextImpl context = new IndexFailureContextImpl();

		context.setThrowable( th );

		// for situation when there is a primary failure
		if ( operationAtFault != null ) {
			context.setOperationAtFault( operationAtFault );
		}
		context.setFailingOperations( getFailingOperations() );
		return context;
	}

	private List<Object> getFailingOperations() {
		if ( failingOperations == null ) {
			failingOperations = new ArrayList<>();
		}
		return failingOperations;
	}

}
