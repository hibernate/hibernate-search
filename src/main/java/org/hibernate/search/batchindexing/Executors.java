package org.hibernate.search.batchindexing;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper class to create threads;
 * these threads are grouped and named to be readily identified in a profiler.
 * 
 * @author Sanne Grinovero
 */
public class Executors {
	
	private static final String THREAD_GROUP_PREFIX = "Hibernate Search indexer: ";
	
	/**
	 * Creates a new fixed size ThreadPoolExecutor
	 * @param threads
	 * @param groupname
	 * @return
	 */
	public static ThreadPoolExecutor newFixedThreadPool(int threads, String groupname) {
		return new ThreadPoolExecutor(
				threads,
				threads,
	            0L, TimeUnit.MILLISECONDS,
	            new LinkedBlockingQueue<Runnable>(),
	            new SearchThreadFactory( groupname ) );
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
            group = (s != null)? s.getThreadGroup() :
                                 Thread.currentThread().getThreadGroup();
            namePrefix = THREAD_GROUP_PREFIX + groupname + "-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, 
                                  namePrefix + threadNumber.getAndIncrement(),
                                  0);
            return t;
        }
        
    }
	
}
