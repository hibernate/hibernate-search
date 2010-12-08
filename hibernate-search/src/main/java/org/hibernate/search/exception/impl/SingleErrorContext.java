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
 * Simplest implementation of an ErrorContext, use it
 * to wrap single exceptions in the backend which are hard to relate
 * to the workqueue as a whole.
 * 
 * @author Sanne Grinovero
 * @since 3.3
 */
public class SingleErrorContext implements ErrorContext {

	private final Throwable throwable;
	
	public SingleErrorContext(Throwable throwable) {
		this.throwable = throwable;
	}

	public List<LuceneWork> getFailingOperations() {
		return Collections.emptyList();
	}
	
	public LuceneWork getOperationAtFault() {
		return null;
	}
	
	public Throwable getThrowable() {
		return throwable;
	}
	
	public boolean hasErrors() {
		return true;
	}
	
}
