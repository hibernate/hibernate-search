/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.infinispan.impl;

import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.engine.service.spi.Stoppable;
import org.hibernate.search.infinispan.logging.impl.Log;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.impl.Executors;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A shared service used among all InfinispanDirectoryProvider instances to
 * delete segments asynchronously.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2014 Red Hat Inc.
 */
public class DefaultAsyncDeleteExecutor implements AsyncDeleteExecutorService, Startable, Stoppable {

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
	public Executor getExecutor() {
		return threadPool;
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

}
