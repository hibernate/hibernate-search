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
import java.util.Map;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.engine.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.search.util.LoggerFactory;
import org.slf4j.Logger;
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
	
	private final List<LuceneWork> queue;
	private final SearchFactoryImplementor searchFactoryImplementor;
	private final Map<DirectoryProvider<?>,PerDPResources> resourcesMap;
	private final boolean sync;
	private final ErrorHandler errorHandler;
	
	private static final DpSelectionVisitor providerSelectionVisitor = new DpSelectionVisitor();
	private static final Logger log = LoggerFactory.make();

	LuceneBackendQueueProcessor(List<LuceneWork> queue,
			SearchFactoryImplementor searchFactoryImplementor,
			Map<DirectoryProvider<?>,PerDPResources> resourcesMap,
			boolean syncMode) {
		this.sync = syncMode;
		this.queue = queue;
		this.searchFactoryImplementor = searchFactoryImplementor;
		this.resourcesMap = resourcesMap;
		this.errorHandler = searchFactoryImplementor.getErrorHandler();
	}

	public void run() {
		QueueProcessors processors = new QueueProcessors( resourcesMap );
		// divide the queue in tasks, adding to QueueProcessors by affected Directory.
		try {
			for ( LuceneWork work : queue ) {
				final Class<?> entityType = work.getEntityClass();
				DocumentBuilderIndexedEntity<?> documentBuilder = searchFactoryImplementor.getDocumentBuilderIndexedEntity( entityType );
				IndexShardingStrategy shardingStrategy = documentBuilder.getDirectoryProviderSelectionStrategy();
				work.getWorkDelegate( providerSelectionVisitor ).addAsPayLoadsToQueue( work, shardingStrategy, processors );
			}
			//this Runnable splits tasks in more runnables and then runs them:
			processors.runAll( sync );
		} catch ( Exception e ) {
			log.error( "Error in backend", e );	
			ErrorContextBuilder builder = new ErrorContextBuilder();
			builder.errorThatOccurred( e );
			errorHandler.handle( builder.createErrorContext() );
		}
	}
	
}
