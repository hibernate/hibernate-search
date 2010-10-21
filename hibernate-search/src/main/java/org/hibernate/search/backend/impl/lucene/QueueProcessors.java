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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.hibernate.search.SearchException;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.store.DirectoryProvider;

/**
 * Container used to split work by DirectoryProviders and execute
 * them concurrently.
 * @author Sanne Grinovero
 */
class QueueProcessors implements PerDirectoryWorkProcessor {
	
	private final Map<DirectoryProvider<?>, PerDPResources> resourcesMap;
	private final Map<DirectoryProvider<?>, PerDPQueueProcessor> dpProcessors
			= new HashMap<DirectoryProvider<?>, PerDPQueueProcessor>();
	
	QueueProcessors(Map<DirectoryProvider<?>, PerDPResources> resourcesMap) {
		this.resourcesMap = resourcesMap;
	}

	public void addWorkToDpProcessor(DirectoryProvider dp, LuceneWork work) {
		if ( ! dpProcessors.containsKey( dp ) ) {
			dpProcessors.put( dp, new PerDPQueueProcessor( resourcesMap.get( dp ) ) );
		}
		PerDPQueueProcessor processor = dpProcessors.get( dp );
		processor.addWork ( work );
	}
	
	/**
	 * Run all index modifications queued so far
	 * @param sync when true this method blocks until all job is done.
	 * @throws InterruptedException only relevant when sync is true.
	 */
	void runAll(boolean sync) throws InterruptedException {
		if ( sync ) {
			runAllWaiting();
		}
		else {
			runAllAsync();
		}
	}
	
	/**
	 * Runs all PerDPQueueProcessor and don't wait for them to finish.
	 */
	private void runAllAsync() {
		// execute all work in parallel on each DirectoryProvider;
		// each DP has it's own ExecutorService.
		for ( PerDPQueueProcessor process : dpProcessors.values() ) {
			ExecutorService executor = process.getOwningExecutor();
			executor.execute( process );
		}
	}

	/**
	 * Runs all PerDPQueueProcessor and waits until all have been processed.
	 * @throws InterruptedException
	 */
	private void runAllWaiting() throws InterruptedException {
		List<Future<Object>> futures = new ArrayList<Future<Object>>( dpProcessors.size() );
		// execute all work in parallel on each DirectoryProvider;
		// each DP has it's own ExecutorService.
		for ( PerDPQueueProcessor process : dpProcessors.values() ) {
			ExecutorService executor = process.getOwningExecutor();
			//wrap each Runnable in a Future
			FutureTask<Object> f = new FutureTask<Object>( process, null );
			futures.add( f );
			executor.execute( f );
		}
		// and then wait for all tasks to be finished:
		for ( Future<Object> f : futures ) {
			if ( !f.isDone() ) {
				try {
					f.get();
				}
				catch (CancellationException ignore) {
					// ignored, as in java.util.concurrent.AbstractExecutorService.invokeAll(Collection<Callable<T>>
					// tasks)
				}
				catch (ExecutionException error) {
					// rethrow cause to serviced thread - this could hide more exception:
					Throwable cause = error.getCause();
					throw new SearchException( cause );
				}
			}
		}
	}

}
