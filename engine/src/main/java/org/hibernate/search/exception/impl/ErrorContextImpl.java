/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.exception.impl;

import java.util.Collections;
import java.util.List;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.exception.ErrorContext;

/**
 * @author Amin Mohammed-Coleman
 * @since 3.2
 */
class ErrorContextImpl implements ErrorContext {

	private List<LuceneWork> failingOperations;

	private LuceneWork operationAtFault;

	private Throwable throwable;

	@Override
	public List<LuceneWork> getFailingOperations() {
		if ( failingOperations == null ) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList( failingOperations );
	}

	@Override
	public LuceneWork getOperationAtFault() {
		return this.operationAtFault;
	}

	@Override
	public Throwable getThrowable() {
		return this.throwable;
	}

	public void setFailingOperations(List<LuceneWork> failingOperations) {
		this.failingOperations = failingOperations;
	}

	public void setThrowable(Throwable th) {
		this.throwable = th;
	}

	public void setOperationAtFault(LuceneWork operationAtFault) {
		this.operationAtFault = operationAtFault;
	}

	@Override
	public boolean hasErrors() {
		return failingOperations != null && failingOperations.size() > 0;
	}

}
