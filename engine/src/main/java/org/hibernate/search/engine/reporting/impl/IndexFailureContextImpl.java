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

	private Object failingOperation;

	private List<Object> uncommittedOperations;

	private Throwable throwable;

	@Override
	public Object getFailingOperation() {
		return this.failingOperation;
	}

	@Override
	public List<Object> getUncommittedOperations() {
		if ( uncommittedOperations == null ) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList( uncommittedOperations );
	}

	@Override
	public Throwable getThrowable() {
		return this.throwable;
	}

	public void setFailingOperation(Object failingOperation) {
		this.failingOperation = failingOperation;
	}

	public void setThrowable(Throwable th) {
		this.throwable = th;
	}

	public void setUncommittedOperations(List<Object> uncommittedOperations) {
		this.uncommittedOperations = uncommittedOperations;
	}

}
