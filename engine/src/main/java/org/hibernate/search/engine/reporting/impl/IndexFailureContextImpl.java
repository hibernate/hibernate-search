/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.reporting.impl;

import java.util.Collections;
import java.util.List;

import org.hibernate.search.engine.reporting.IndexFailureContext;

public class IndexFailureContextImpl implements IndexFailureContext {

	private List<Object> failingOperations;

	private Object operationAtFault;

	private Throwable throwable;

	@Override
	public List<Object> getFailingOperations() {
		if ( failingOperations == null ) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList( failingOperations );
	}

	@Override
	public Object getOperationAtFault() {
		return this.operationAtFault;
	}

	@Override
	public Throwable getThrowable() {
		return this.throwable;
	}

	public void setFailingOperations(List<Object> failingOperations) {
		this.failingOperations = failingOperations;
	}

	public void setThrowable(Throwable th) {
		this.throwable = th;
	}

	public void setOperationAtFault(Object operationAtFault) {
		this.operationAtFault = operationAtFault;
	}

}
