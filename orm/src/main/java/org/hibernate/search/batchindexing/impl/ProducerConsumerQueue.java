/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batchindexing.impl;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implements a blocking queue capable of storing
 * a "poison" token to signal consumer threads
 * that the task is finished.
 *
 * @author Sanne Grinovero
 */
public class ProducerConsumerQueue<T> {

	private static final int DEFAULT_BUFF_LENGHT = 1000;
	private static final Object exitToken = new Object();

	//doesn't use generics here as exitToken needs to be put in the queue too:
	@SuppressWarnings("unchecked")
	private final BlockingQueue queue;
	private final AtomicInteger producersToWaitFor;

	/**
	 * @param producersToWaitFor The number of producer threads.
	 */
	public ProducerConsumerQueue( int producersToWaitFor ) {
		this( DEFAULT_BUFF_LENGHT, producersToWaitFor );
	}

	@SuppressWarnings("unchecked")
	public ProducerConsumerQueue( int queueLenght, int producersToWaitFor ) {
		queue = new ArrayBlockingQueue( queueLenght );
		this.producersToWaitFor = new AtomicInteger( producersToWaitFor );
	}

	/**
	 * Blocks until an object is available; when null
	 * is returned the client thread should quit.
	 * @return the next object in the queue, or null to exit
	 * @throws InterruptedException
	 */
	@SuppressWarnings("unchecked")
	public T take() throws InterruptedException {
		Object obj = queue.take();
		if ( obj == exitToken ) {
			//restore exit signal for other threads
			queue.put( exitToken );
			return null;
		}
		else {
			return (T)obj;
		}
	}

	/**
	 * Adds a new object to the queue, blocking if no space is
	 * available.
	 * @param obj
	 * @throws InterruptedException
	 */
	@SuppressWarnings("unchecked")
	public void put(T obj) throws InterruptedException {
		queue.put( obj );
	}

	/**
	 * Each producer thread should call producerStopping() when it has
	 * finished. After doing it can safely terminate.
	 * After all producer threads have called producerStopping()
	 * a token will be inserted in the blocking queue to eventually
	 * awake sleeping consumers and have them quit, after the
	 * queue has been processed.
	 */
	@SuppressWarnings("unchecked")
	public void producerStopping() {
		int activeProducers = producersToWaitFor.decrementAndGet();
		//last producer must close consumers
		if ( activeProducers == 0 ) {
			try {
				queue.put( exitToken );//awake all waiting threads to let them quit.
			}
			catch (InterruptedException e) {
				//just quit, consumers will be interrupted anyway if it's a shutdown.
				Thread.currentThread().interrupt();
			}
		}
	}

}
