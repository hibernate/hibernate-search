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
		return new ThreadPoolExecutor(
				threads,
				threads,
	            0L, TimeUnit.MILLISECONDS,
	            new LinkedBlockingQueue<Runnable>( QUEUE_MAX_LENGTH ),
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
