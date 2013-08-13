/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
