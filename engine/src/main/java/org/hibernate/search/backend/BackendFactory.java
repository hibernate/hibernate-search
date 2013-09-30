/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.backend;

import java.util.Properties;
import java.util.concurrent.ExecutorService;

import org.hibernate.search.util.StringHelper;
import org.hibernate.search.Environment;
import org.hibernate.search.backend.impl.blackhole.BlackHoleBackendQueueProcessor;
import org.hibernate.search.backend.impl.jgroups.AutoNodeSelector;
import org.hibernate.search.backend.impl.jgroups.JGroupsBackendQueueProcessor;
import org.hibernate.search.backend.impl.jgroups.MasterNodeSelector;
import org.hibernate.search.backend.impl.jgroups.SlaveNodeSelector;
import org.hibernate.search.backend.impl.jms.JndiJMSBackendQueueProcessor;
import org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessor;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.batchindexing.impl.Executors;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.impl.ClassLoaderHelper;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public final class BackendFactory {

	private BackendFactory() {
		//not allowed
	}

	public static BackendQueueProcessor createBackend(DirectoryBasedIndexManager indexManager, WorkerBuildContext context, Properties properties) {
		String backend = properties.getProperty( Environment.WORKER_BACKEND );
		return createBackend( backend, indexManager, context, properties );
	}

	public static BackendQueueProcessor createBackend(String backend, DirectoryBasedIndexManager indexManager, WorkerBuildContext context,
			Properties properties) {

		final BackendQueueProcessor backendQueueProcessor;

		if ( StringHelper.isEmpty( backend ) || "lucene".equalsIgnoreCase( backend ) ) {
			backendQueueProcessor = new LuceneBackendQueueProcessor();
		}
		else if ( "jms".equalsIgnoreCase( backend ) ) {
			backendQueueProcessor = new JndiJMSBackendQueueProcessor();
		}
		else if ( "blackhole".equalsIgnoreCase( backend ) ) {
			backendQueueProcessor = new BlackHoleBackendQueueProcessor();
		}
		else if ( "jgroupsMaster".equals( backend ) ) {
			backendQueueProcessor = new JGroupsBackendQueueProcessor( new MasterNodeSelector() );
		}
		else if ( "jgroupsSlave".equals( backend ) ) {
			backendQueueProcessor = new JGroupsBackendQueueProcessor( new SlaveNodeSelector() );
		}
		else if ( "jgroups".equals( backend ) ) {
			backendQueueProcessor = new JGroupsBackendQueueProcessor( new AutoNodeSelector( indexManager.getIndexName() ) );
		}
		else {
			backendQueueProcessor = ClassLoaderHelper.instanceFromName(
					BackendQueueProcessor.class,
					backend, BackendFactory.class, "processor"
			);
		}
		backendQueueProcessor.initialize( properties, context, indexManager );
		return backendQueueProcessor;
	}

	/**
	 * @param properties the configuration to parse
	 *
	 * @return true if the configuration uses sync indexing
	 */
	public static boolean isConfiguredAsSync(Properties properties) {
		// default to sync if none defined
		return !"async".equalsIgnoreCase( properties.getProperty( Environment.WORKER_EXECUTION ) );
	}

	/**
	 * Builds an ExecutorService to run backend work.
	 *
	 * @param properties Might optionally contain configuration options for the ExecutorService
	 * @param indexManagerName The indexManager going to be linked to this ExecutorService
	 * @return null if the work needs execution in sync
	 */
	public static ExecutorService buildWorkersExecutor(Properties properties, String indexManagerName) {
		int threadPoolSize = getWorkerThreadPoolSize( properties );
		int queueSize = getWorkerQueueSize( properties );
		return Executors.newFixedThreadPool(
				threadPoolSize,
				"IndexWriter worker executor for " + indexManagerName,
				queueSize
		);
	}

	public static int getWorkerThreadPoolSize(Properties properties) {
		//default to a simple asynchronous operation
		return ConfigurationParseHelper.getIntValue( properties, Environment.WORKER_THREADPOOL_SIZE, 1 );
	}

	public static int getWorkerQueueSize(Properties properties) {
		//no queue limit
		return ConfigurationParseHelper.getIntValue( properties, Environment.WORKER_WORKQUEUE_SIZE, Integer.MAX_VALUE );
	}

}
