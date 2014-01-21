/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.batchindexing.impl;

import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Common parent of all Runnable implementations for the batch indexing:
 * share the code for handling runtime exceptions.
 */
abstract class ErrorHandledRunnable implements Runnable {

	private static final Log log = LoggerFactory.make();

	protected final SearchFactoryImplementor searchFactoryImplementor;

	protected ErrorHandledRunnable(SearchFactoryImplementor searchFactoryImplementor) {
		this.searchFactoryImplementor = searchFactoryImplementor;
	}

	@Override
	public final void run() {
		ErrorHandler errorHandler = searchFactoryImplementor.getErrorHandler();
		try {
			runWithErrorHandler();
		}
		catch (Exception re) {
			//being this an async thread we want to make sure everything is somehow reported
			errorHandler.handleException( log.massIndexerUnexpectedErrorMessage() , re );
			cleanUpOnError();
		}
	}

	protected abstract void runWithErrorHandler() throws Exception;

	protected void cleanUpOnError() {
		//no-op unless overridden
	}

}
