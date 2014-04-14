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

import java.lang.reflect.Constructor;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import org.hibernate.search.cfg.Environment;
import org.hibernate.search.backend.impl.blackhole.BlackHoleBackendQueueProcessor;
import org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessor;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.batchindexing.impl.Executors;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Factory to instantiate the correct Search backend or to be more concrete the {@link BackendQueueProcessor} implementation.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 * @author Hardy Ferentschik
 */
public final class BackendFactory {
	private static final Log log = LoggerFactory.make();

	// TODO - Consider using service mechanism instead of reflection to instantiate backends
	// need to allow multiple service impl first and provide a way for selecting one (HF)
	private static final String JMS_BACKEND_QUEUE_PROCESSOR = "org.hibernate.search.backend.jms.impl.JndiJMSBackendQueueProcessor";
	private static final String JGROUPS_BACKEND_QUEUE_PROCESSOR = "org.hibernate.search.backend.jgroups.impl.JGroupsBackendQueueProcessor";
	private static final String JGROUPS_MASTER_SELECTOR = "org.hibernate.search.backend.jgroups.impl.MasterNodeSelector";
	private static final String JGROUPS_SLAVE_SELECTOR = "org.hibernate.search.backend.jgroups.impl.SlaveNodeSelector";
	private static final String JGROUPS_AUTO_SELECTOR = "org.hibernate.search.backend.jgroups.impl.AutoNodeSelector";
	private static final String JGROUPS_SELECTOR_BASE_TYPE = "org.hibernate.search.backend.jgroups.impl.NodeSelectorStrategy";

	private BackendFactory() {
		//not allowed
	}

	public static BackendQueueProcessor createBackend(DirectoryBasedIndexManager indexManager, WorkerBuildContext buildContext, Properties properties) {
		String backend = properties.getProperty( Environment.WORKER_BACKEND );
		return createBackend( backend, indexManager, buildContext, properties );
	}

	public static BackendQueueProcessor createBackend(String backend,
			DirectoryBasedIndexManager indexManager,
			WorkerBuildContext buildContext,
			Properties properties) {
		final BackendQueueProcessor backendQueueProcessor;

		if ( StringHelper.isEmpty( backend ) || "lucene".equalsIgnoreCase( backend ) ) {
			backendQueueProcessor = new LuceneBackendQueueProcessor();
		}
		else if ( "jms".equalsIgnoreCase( backend ) ) {
			backendQueueProcessor = ClassLoaderHelper.instanceFromName(
					BackendQueueProcessor.class,
					JMS_BACKEND_QUEUE_PROCESSOR,
					"JMS backend ",
					buildContext.getServiceManager()
			);
		}
		else if ( "blackhole".equalsIgnoreCase( backend ) ) {
			backendQueueProcessor = new BlackHoleBackendQueueProcessor();
		}
		else if ( "jgroupsMaster".equals( backend ) ) {
			backendQueueProcessor = createJGroupsQueueProcessor( JGROUPS_MASTER_SELECTOR, buildContext );
		}
		else if ( "jgroupsSlave".equals( backend ) ) {
			backendQueueProcessor = createJGroupsQueueProcessor( JGROUPS_SLAVE_SELECTOR, buildContext );
		}
		else if ( "jgroups".equals( backend ) ) {
			Class<?> selectorClass = ClassLoaderHelper.classForName(
					JGROUPS_AUTO_SELECTOR,
					"JGroups node selector ",
					buildContext.getServiceManager()
			);

			final Constructor constructor;
			final Object autoNodeSelector;
			try {
				constructor = selectorClass.getConstructor( String.class );
				autoNodeSelector = constructor.newInstance( indexManager.getIndexName() );
			}
			catch (Exception e) {
				throw log.getUnableToCreateJGroupsBackendException( e );
			}
			backendQueueProcessor = createJGroupsQueueProcessor( autoNodeSelector, buildContext.getServiceManager() );
		}
		else {
			ServiceManager serviceManager = buildContext.getServiceManager();
			backendQueueProcessor = ClassLoaderHelper.instanceFromName(
					BackendQueueProcessor.class,
					backend,
					"processor",
					serviceManager
			);
		}
		backendQueueProcessor.initialize( properties, buildContext, indexManager );
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
	 *
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

	private static BackendQueueProcessor createJGroupsQueueProcessor(String selectorClass, BuildContext buildContext) {
		ServiceManager serviceManager = buildContext.getServiceManager();
		return createJGroupsQueueProcessor(
				ClassLoaderHelper.instanceFromName(
						Object.class,
						selectorClass,
						"JGroups node selector",
						serviceManager
				), serviceManager
		);
	}

	private static BackendQueueProcessor createJGroupsQueueProcessor(Object selectorInstance, ServiceManager serviceManager) {
		BackendQueueProcessor backendQueueProcessor;
		Class<?> processorClass = ClassLoaderHelper.classForName(
				JGROUPS_BACKEND_QUEUE_PROCESSOR,
				"JGroups backend ",
				serviceManager
		);

		Class<?> selectorClass = ClassLoaderHelper.classForName(
				JGROUPS_SELECTOR_BASE_TYPE,
				"JGroups node selector ",
				serviceManager
		);

		Constructor constructor;
		try {
			constructor = processorClass.getConstructor( selectorClass );
			backendQueueProcessor = (BackendQueueProcessor) constructor.newInstance( selectorInstance );
		}
		catch (Exception e) {
			throw log.getUnableToCreateJGroupsBackendException( e );
		}
		return backendQueueProcessor;
	}

}
