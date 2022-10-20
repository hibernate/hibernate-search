/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.work.execution.impl;

import java.lang.invoke.MethodHandles;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

import org.hibernate.search.engine.backend.work.execution.spi.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.WorkQueueFullException;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public enum OperationSubmitterType implements OperationSubmitter {
	/**
	 * {@code OperationSubmitter} implementation that will block the process and wait for the operation to be submitted
	 * to the queue.
	 */
	BLOCKING {
		@Override
		public <T> void submitToQueue(Queue<T> queue, T element) {
			if ( queue instanceof BlockingQueue ) {
				try {
					( (BlockingQueue<T>) queue ).put( element );
				}
				catch (InterruptedException e) {
					throw log.failedToSubmitToQueue( element, e.getMessage(), e );
				}
			}
			else {
				queue.add( element );
			}
		}
	},
	/**
	 * {@code OperationSubmitter} implementation that will reject the operation by throwing {@link WorkQueueFullException}
	 * if the underlying queue is full and cannot accept more elements at the moment.
	 */
	REJECTING_EXECUTION_EXCEPTION {
		@Override
		public <T> void submitToQueue(Queue<T> queue, T element) {
			try {
				queue.add( element );
			}
			catch (IllegalStateException e) {
				//means we had a blocking queue, and it's full:
				throw log.cannotAcceptMoreWork( element, e.getMessage(), e );
			}
		}
	},
	;

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );
}
