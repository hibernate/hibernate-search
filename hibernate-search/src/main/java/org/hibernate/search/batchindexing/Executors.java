/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.batchindexing;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper class to create threads;
 * these threads are grouped and named to be identified in a profiler.
 * 
 * @author Sanne Grinovero
 */
public class Executors {
	
	private static final String THREAD_GROUP_PREFIX = "Hibernate Search: ";
	private static final int QUEUE_MAX_LENGTH = 1000; //TODO have it configurable?
	
	/**
	 * Creates a new fixed size ThreadPoolExecutor.
	 * It's using a blockingqueue of maximum 1000 elements and the rejection
	 * policy is set to CallerRunsPolicy for the case the queue is full.
	 * These settings are required to cap the queue, to make sure the
	 * timeouts are reasonable for most jobs.
	 * 
	 * @param threads the number of threads
	 * @param groupname a label to identify the threadpool; useful for profiling.
	 * @return the new ExecutorService
	 */
	public static ThreadPoolExecutor newFixedThreadPool(int threads, String groupname) {
		return newFixedThreadPool( threads, groupname, QUEUE_MAX_LENGTH );
	}

    /**
     * Creates a new fixed size ThreadPoolExecutor
     * @param threads the number of threads
     * @param groupname a label to identify the threadpool; useful for profiling.
     * @param queueSize the size of the queue to store Runnables when all threads are busy
     * @return the new ExecutorService
     */
	public static ThreadPoolExecutor newFixedThreadPool(int threads, String groupname, int queueSize) {
		return new ThreadPoolExecutor(
				threads,
				threads,
	            0L, TimeUnit.MILLISECONDS,
	            new LinkedBlockingQueue<Runnable>( queueSize ),
	            new SearchThreadFactory( groupname ),
	            new ThreadPoolExecutor.CallerRunsPolicy() );
	}
	
	/**
     * The thread factory, used to customize thread names
     */
    private static class SearchThreadFactory implements ThreadFactory {
    	
        final ThreadGroup group;
        final AtomicInteger threadNumber = new AtomicInteger( 1 );
        final String namePrefix;

        SearchThreadFactory(String groupname) {
            SecurityManager s = System.getSecurityManager();
            group = ( s != null ) ? s.getThreadGroup() :
                                 Thread.currentThread().getThreadGroup();
            namePrefix = THREAD_GROUP_PREFIX + groupname + "-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread( group, r, 
                                  namePrefix + threadNumber.getAndIncrement(),
                                  0 );
            return t;
        }
        
    }
	
}
