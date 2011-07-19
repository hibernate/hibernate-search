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
package org.hibernate.search.backend.impl.lucene;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.impl.ErrorContextBuilder;

/**
 * Apply the operations to Lucene directories.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author John Griffin
 * @author Sanne Grinovero
 */
class LuceneBackendQueueProcessor implements Runnable {
	
	private final boolean sync;
	private final ErrorHandler errorHandler;
	private final PerDPQueueProcessor dpProcessors;
	
	private static final Log log = LoggerFactory.make();

	LuceneBackendQueueProcessor(List<LuceneWork> queue,
			SearchFactoryImplementor searchFactoryImplementor,
			PerDPResources resourcesMap,
			boolean syncMode) {
		this.sync = syncMode;
		this.errorHandler = searchFactoryImplementor.getErrorHandler();
		this.dpProcessors = new PerDPQueueProcessor( resourcesMap, queue );
	}

	public void run() {
		ExecutorService executor = dpProcessors.getOwningExecutor();
		try {
			if ( sync ) {
				//even in sync operations we always delegate to a single thread,
				//to make sure all index operations are applied in sequence.
				Future<?> future = executor.submit( dpProcessors );
				future.get();
			}
			else {
				executor.execute( dpProcessors );	
			}
		} catch ( Exception e ) {
			log.backendError( e );
			ErrorContextBuilder builder = new ErrorContextBuilder();
			builder.errorThatOccurred( e );
			errorHandler.handle( builder.createErrorContext() );
		}
	}
	
}
