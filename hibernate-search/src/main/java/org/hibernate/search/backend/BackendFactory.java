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

import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.search.Environment;
import org.hibernate.search.backend.impl.blackhole.BlackHoleBackendQueueProcessorFactory;
import org.hibernate.search.backend.impl.jgroups.MasterJGroupsBackendQueueProcessorFactory;
import org.hibernate.search.backend.impl.jgroups.SlaveJGroupsBackendQueueProcessorFactory;
import org.hibernate.search.backend.impl.jms.JMSBackendQueueProcessorFactory;
import org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessorFactory;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.batchindexing.impl.Executors;
import org.hibernate.search.indexes.serialization.codex.avro.impl.AvroSerializationProvider;
import org.hibernate.search.indexes.serialization.codex.impl.PluggableSerializationLuceneWorkSerializer;
import org.hibernate.search.indexes.serialization.codex.spi.LuceneWorkSerializer;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.impl.ClassLoaderHelper;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class BackendFactory {
	
	public static BackendQueueProcessor createBackend(IndexManager indexManager, WorkerBuildContext context, Properties properties) {

		String backend = properties.getProperty( Environment.WORKER_BACKEND );
		
		final BackendQueueProcessor backendQueueProcessorFactory;
		
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
			backendQueueProcessorFactory = ClassLoaderHelper.instanceFromName(
					BackendQueueProcessor.class,
					backend, BackendFactory.class, "processor"
			);
		}
		backendQueueProcessorFactory.initialize( properties, context, indexManager );
		return backendQueueProcessorFactory;
	}
	
	/**
	 * @param properties the configuration to parse
	 * @return true if the configuration uses sync indexing
	 */
	public static boolean isConfiguredAsSync(Properties properties) {
		// default to sync if none defined
		return !"async".equalsIgnoreCase( properties.getProperty( Environment.WORKER_EXECUTION ) );
	}
	
	/**
	 * Builds an ExecutorService to run backend work. 
	 * @param properties Might optionally contain configuration options for the ExecutorService
	 * @param indexManagerName The indexManager going to be linked to this ExecutorService
	 * @return null if the work needs execution in sync
	 */
	public static ExecutorService buildWorkersExecutor(Properties properties, String indexManagerName) {
		int threadPoolSize = getWorkerThreadPoolSize( properties );
		int queueSize = getWorkerQueueSize( properties );
		return Executors.newFixedThreadPool( threadPoolSize,
				"backend queueing processor for index " + indexManagerName,
				queueSize );
	}
	
	public static int getWorkerThreadPoolSize(Properties properties) {
		//default to a simple asynchronous operation
		return ConfigurationParseHelper.getIntValue( properties, Environment.WORKER_THREADPOOL_SIZE, 1 );
	}
	
	public static int getWorkerQueueSize(Properties properties) {
		//no queue limit
		return ConfigurationParseHelper.getIntValue( properties, Environment.WORKER_WORKQUEUE_SIZE, Integer.MAX_VALUE );
	}
	
	public static int getWorkerBatchSize(Properties properties) {
		return ConfigurationParseHelper.getIntValue( properties, Environment.QUEUEINGPROCESSOR_BATCHSIZE, 0 );
	}

	public static LuceneWorkSerializer createSerializer(String indexName, Properties cfg,
			WorkerBuildContext buildContext) {
		return new PluggableSerializationLuceneWorkSerializer(
				new AvroSerializationProvider(),
				buildContext.getUninitializedSearchFactory() );
	}

}
