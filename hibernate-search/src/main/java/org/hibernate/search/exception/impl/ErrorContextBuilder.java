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

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.exception.ErrorContext;

/**
 * @author Amin Mohammed-Coleman
 * @since 3.2
 */
public class ErrorContextBuilder {

	private Throwable th;
	private List<LuceneWork> workToBeDone;
	private List<LuceneWork> failingOperations = new ArrayList<LuceneWork>();
	private List<LuceneWork> operationsThatWorked = new ArrayList<LuceneWork>();

	public ErrorContextBuilder errorThatOccurred(Throwable th) {
		this.th = th;
		return this;
	}

	public ErrorContextBuilder addAllWorkThatFailed(List<LuceneWork> worksThatFailed) {
		this.failingOperations.addAll( worksThatFailed );
		return this;
	}

	public ErrorContextBuilder workCompleted(LuceneWork luceneWork) {
		this.operationsThatWorked.add( luceneWork );
		return this;

	}

	public ErrorContextBuilder allWorkToBeDone(List<LuceneWork> workOnWriter) {
		this.workToBeDone = new ArrayList<LuceneWork>( workOnWriter );
		return this;
	}

	public ErrorContext createErrorContext() {
		ErrorContextImpl context = new ErrorContextImpl();

		context.setThrowable( th );

		// for situation when there is a primary failure
		if ( workToBeDone != null ) {
			List<LuceneWork> workLeft = new ArrayList<LuceneWork>( workToBeDone );
			if ( !operationsThatWorked.isEmpty() ) {
				workLeft.removeAll( operationsThatWorked );
			}

			if ( !workLeft.isEmpty() ) {
				context.setOperationAtFault( workLeft.remove( 0 ) );
				failingOperations.addAll( workLeft );
			}
		}
		context.setFailingOperations( failingOperations );
		return context;
	}

}
