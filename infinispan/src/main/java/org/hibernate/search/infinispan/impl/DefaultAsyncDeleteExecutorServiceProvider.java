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
package org.hibernate.search.infinispan.impl;

import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.batchindexing.impl.Executors;
import org.hibernate.search.infinispan.logging.impl.Log;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A shared service used among all InfinispanDirectoryProvider instances to
 * delete segments asynchronously.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2014 Red Hat Inc.
 */
public class DefaultAsyncDeleteExecutorServiceProvider implements AsyncDeleteExecutorService {

	private static final Log log = LoggerFactory.make( Log.class );

	private ThreadPoolExecutor threadPool;

	@Override
	public void start(Properties properties, BuildContext context) {
		threadPool = Executors
				.newScalableThreadPool( 1, 5, "async deletion of index segments", 100 );
	}

	@Override
	public void stop() {
		closeAndFlush();
	}

	@Override
	public AsyncDeleteExecutorService getService() {
		return this;
	}

	@Override
	public void closeAndFlush() {
		//Each DirectoryProvider using this service should flush and wait a bit to allow
		//async work to be performed before the Directory itself becomes unavailable.
		threadPool.shutdown();
		try {
			threadPool.awaitTermination( 30L, TimeUnit.SECONDS );
		}
		catch (InterruptedException e) {
			log.interruptedWhileWaitingForAsyncDeleteFlush();
		}
	}

	@Override
	public Executor getExecutor() {
		return threadPool;
	}

}
