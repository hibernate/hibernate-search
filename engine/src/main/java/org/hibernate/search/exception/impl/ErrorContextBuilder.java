/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.exception.impl;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.exception.ErrorContext;

/**
 * @author Amin Mohammed-Coleman
 * @since 3.2
 */
public class ErrorContextBuilder {

	private Throwable th;
	private Iterable<LuceneWork> workToBeDone;
	private List<LuceneWork> failingOperations;
	private List<LuceneWork> operationsThatWorked;

	public ErrorContextBuilder errorThatOccurred(Throwable th) {
		this.th = th;
		return this;
	}

	public ErrorContextBuilder addWorkThatFailed(LuceneWork failedWork) {
		this.getFailingOperations().add( failedWork );
		return this;
	}

	public ErrorContextBuilder addAllWorkThatFailed(List<LuceneWork> worksThatFailed) {
		this.getFailingOperations().addAll( worksThatFailed );
		return this;
	}

	public ErrorContextBuilder workCompleted(LuceneWork luceneWork) {
		this.getOperationsThatWorked().add( luceneWork );
		return this;

	}

	public ErrorContextBuilder allWorkToBeDone(Iterable<LuceneWork> workOnWriter) {
		this.workToBeDone = workOnWriter;
		return this;
	}

	public ErrorContext createErrorContext() {
		ErrorContextImpl context = new ErrorContextImpl();

		context.setThrowable( th );

		// for situation when there is a primary failure
		if ( workToBeDone != null ) {
			List<LuceneWork> workLeft = new ArrayList<LuceneWork>();
			for ( LuceneWork work : workToBeDone ) {
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

	private List<LuceneWork> getFailingOperations() {
		if ( failingOperations == null ) {
			failingOperations = new ArrayList<LuceneWork>();
		}
		return failingOperations;
	}

	private List<LuceneWork> getOperationsThatWorked() {
		if ( operationsThatWorked == null ) {
			operationsThatWorked = new LinkedList<LuceneWork>();
		}
		return operationsThatWorked;
	}

}
