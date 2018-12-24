/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import java.util.Collections;
import java.util.List;

import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.common.spi.ErrorContext;

public class ErrorContextImpl implements ErrorContext {

	private List<Object> failingOperations;

	private Object operationAtFault;

	private Throwable throwable;

	private IndexManager indexManager;

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

	@Override
	public boolean hasErrors() {
		return failingOperations != null && failingOperations.size() > 0;
	}

	@Override
	public IndexManager getIndexManager() {
		return indexManager;
	}

	public void setIndexManager(IndexManager indexManager) {
		this.indexManager = indexManager;
	}

}
