/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.reporting.spi;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.impl.FailureContextImpl;

public class FailureContextBuilder {

	private Throwable th;
	private Object operationAtFault;
	private Iterable<Object> workToBeDone;
	private List<Object> failingOperations;
	private List<Object> operationsThatWorked;

	public FailureContextBuilder throwable(Throwable th) {
		this.th = th;
		return this;
	}

	public FailureContextBuilder operationAtFault(Object operationAtFault) {
		this.operationAtFault = operationAtFault;
		return this;
	}

	public FailureContextBuilder addWorkThatFailed(Object failedWork) {
		this.getFailingOperations().add( failedWork );
		return this;
	}

	public FailureContextBuilder addAllWorkThatFailed(List<Object> worksThatFailed) {
		this.getFailingOperations().addAll( worksThatFailed );
		return this;
	}

	public FailureContextBuilder workCompleted(Object luceneWork) {
		this.getOperationsThatWorked().add( luceneWork );
		return this;

	}

	public FailureContextBuilder allWorkToBeDone(Iterable<Object> workOnWriter) {
		this.workToBeDone = workOnWriter;
		return this;
	}

	public FailureContext createFailureContext() {
		FailureContextImpl context = new FailureContextImpl();

		context.setThrowable( th );

		// for situation when there is a primary failure
		if ( operationAtFault != null ) {
			context.setOperationAtFault( operationAtFault );
		}
		else if ( workToBeDone != null ) {
			List<Object> workLeft = new ArrayList<>();
			for ( Object work : workToBeDone ) {
				workLeft.add( work );
			}
			if ( operationsThatWorked != null ) {
				workLeft.removeAll( operationsThatWorked );
			}

			if ( !workLeft.isEmpty() ) {
				context.setOperationAtFault( workLeft.remove( 0 ) );
				getFailingOperations().addAll( workLeft );
			}
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

	private List<Object> getOperationsThatWorked() {
		if ( operationsThatWorked == null ) {
			operationsThatWorked = new LinkedList<>();
		}
		return operationsThatWorked;
	}

}
