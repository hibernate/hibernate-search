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
package org.hibernate.search.backend.impl;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.search.Environment;
import org.hibernate.search.SearchException;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.BackendQueueProcessorFactory;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.QueueingProcessor;
import org.hibernate.search.backend.Work;
import org.hibernate.search.backend.WorkQueue;
import org.hibernate.search.backend.WorkType;
import org.hibernate.search.backend.configuration.ConfigurationParseHelper;
import org.hibernate.search.backend.impl.blackhole.BlackHoleBackendQueueProcessorFactory;
import org.hibernate.search.backend.impl.jgroups.MasterJGroupsBackendQueueProcessorFactory;
import org.hibernate.search.backend.impl.jgroups.SlaveJGroupsBackendQueueProcessorFactory;
import org.hibernate.search.backend.impl.jms.JMSBackendQueueProcessorFactory;
import org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessorFactory;
import org.hibernate.search.batchindexing.Executors;
import org.hibernate.search.engine.AbstractDocumentBuilder;
import org.hibernate.search.engine.DocumentBuilderContainedEntity;
import org.hibernate.search.engine.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.engine.WorkPlan;
import org.hibernate.search.util.ClassLoaderHelper;
import org.hibernate.search.util.LoggerFactory;

/**
 * Batch work until {@link #performWorks} is called.
 * The work is then executed synchronously or asynchronously.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class BatchedQueueingProcessor implements QueueingProcessor {

	private static final Logger log = LoggerFactory.make();

	private final boolean sync;
	private final int batchSize;
	private final ExecutorService executorService;
	private final BackendQueueProcessorFactory backendQueueProcessorFactory;
	private final SearchFactoryImplementor searchFactoryImplementor;

	public BatchedQueueingProcessor(WorkerBuildContext context, Properties properties) {
		this.sync = isConfiguredAsSync( properties );

		//default to a simple asynchronous operation
		int threadPoolSize = ConfigurationParseHelper.getIntValue( properties, Environment.WORKER_THREADPOOL_SIZE, 1 );
		//no queue limit
		int queueSize = ConfigurationParseHelper.getIntValue(
				properties, Environment.WORKER_WORKQUEUE_SIZE, Integer.MAX_VALUE
		);

		batchSize = ConfigurationParseHelper.getIntValue( properties, Environment.WORKER_BATCHSIZE, 0 );

		if ( !sync ) {
			/**
			 * If the queue limit is reached, the operation is executed by the main thread
			 */
			executorService =  Executors.newFixedThreadPool( threadPoolSize, "backend queueing processor", queueSize );
		}
		else {
			executorService = null;
		}
		String backend = properties.getProperty( Environment.WORKER_BACKEND );
		if ( StringHelper.isEmpty( backend ) || "lucene".equalsIgnoreCase( backend ) ) {
			backendQueueProcessorFactory = new LuceneBackendQueueProcessorFactory();
		}
		else if ( "jms".equalsIgnoreCase( backend ) ) {
			backendQueueProcessorFactory = new JMSBackendQueueProcessorFactory();
		}
		else if ( "blackhole".equalsIgnoreCase( backend ) ) {
			backendQueueProcessorFactory = new BlackHoleBackendQueueProcessorFactory();
		}
		else if ( "jgroupsMaster".equals( backend ) ) {
				backendQueueProcessorFactory = new MasterJGroupsBackendQueueProcessorFactory();
		}
		else if ( "jgroupsSlave".equals( backend ) ) {
				backendQueueProcessorFactory = new SlaveJGroupsBackendQueueProcessorFactory();
		}
		else {
			backendQueueProcessorFactory = ClassLoaderHelper.instanceFromName( BackendQueueProcessorFactory.class,
					backend, BatchedQueueingProcessor.class, "processor" );
		}
		backendQueueProcessorFactory.initialize( properties, context );
		context.setBackendQueueProcessorFactory( backendQueueProcessorFactory );
		this.searchFactoryImplementor = context.getUninitializedSearchFactory();
	}

	public void add(Work work, WorkQueue workQueue) {
		//don't check for builder it's done in prepareWork
		//FIXME WorkType.COLLECTION does not play well with batchSize
		workQueue.add( work );
		if ( batchSize > 0 && workQueue.size() >= batchSize ) {
			WorkQueue subQueue = workQueue.splitQueue();
			prepareWorks( subQueue );
			performWorks( subQueue );
		}
	}

	public void prepareWorks(WorkQueue workQueue) {
		final boolean alreadyProcessedAndUnchanged = workQueue.isSealedAndUnchanged();
		if ( !alreadyProcessedAndUnchanged ) { 
			WorkPlan plan = new WorkPlan();
			for (Work work : workQueue.getQueue()){
				plan.addWork( work );
			}
			plan.processContainedIn( searchFactoryImplementor );
			List<LuceneWork> luceneWorkPlan = plan.getPlannedLuceneWork( searchFactoryImplementor );
			workQueue.setSealedQueue( luceneWorkPlan );
		}
	}

	public void performWorks(WorkQueue workQueue) {
		List<LuceneWork> sealedQueue = workQueue.getSealedQueue();
		if ( log.isTraceEnabled() ) {
			StringBuilder sb = new StringBuilder( "Lucene WorkQueue to send to backend:[ \n\t" );
			for ( LuceneWork lw : sealedQueue ) {
				sb.append( lw.toString() );
				sb.append( "\n\t" );
			}
			if ( sealedQueue.size() > 0 ) {
				sb.deleteCharAt( sb.length() - 1 );
			}
			sb.append( "]" );
			log.trace( sb.toString() );
		}
		Runnable processor = backendQueueProcessorFactory.getProcessor( sealedQueue );
		if ( sync ) {
			processor.run();
		}
		else {
			executorService.execute( processor );
		}
	}

	public void cancelWorks(WorkQueue workQueue) {
		workQueue.clear();
	}

	public void close() {
		//gracefully stop
		if ( executorService != null && !executorService.isShutdown() ) {
			executorService.shutdown();
			try {
				executorService.awaitTermination( Long.MAX_VALUE, TimeUnit.SECONDS );
			}
			catch ( InterruptedException e ) {
				log.error( "Unable to properly shut down asynchronous indexing work", e );
			}
		}
		//and stop the backend
		backendQueueProcessorFactory.close();
	}

	/**
	 * @param properties the configuration to parse
	 * @return true if the configuration uses sync indexing
	 */
	public static boolean isConfiguredAsSync(Properties properties) {
		// default to sync if none defined
		return !"async".equalsIgnoreCase( properties.getProperty( Environment.WORKER_EXECUTION ) );
	}

}
