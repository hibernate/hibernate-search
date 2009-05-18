package org.hibernate.search.batchindexing;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import org.hibernate.search.backend.BackendQueueProcessorFactory;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.util.LoggerFactory;
import org.slf4j.Logger;

/**
 * This Runnable is meant to push to the backend
 * all LuceneWork it can take from the ProducerConsumerQueue.
 * 
 * FIXME:
 * Is currently more an adaptor to bridge the queue approach
 * to the current backend implementations. This is not by any means
 * the most efficient way to index documents, it has been built only
 * to test the approach.
 * 
 */
class IndexWritingJob implements Runnable {
	
	private static final Logger log = LoggerFactory.make();
	
	private final ProducerConsumerQueue pleq;
	private final BackendQueueProcessorFactory backend;
	private final CountDownLatch endSignal;
	private final IndexerProgressMonitor monitor;
	
	/**
	 * @param pleq This queue contains the LuceneWork to be send to the backend
	 * @param endSignal
	 * @param monitor
	 * @param backendQueueProcessorFactory
	 */
	IndexWritingJob(ProducerConsumerQueue pleq, CountDownLatch endSignal, IndexerProgressMonitor monitor, BackendQueueProcessorFactory backendQueueProcessorFactory) {
		this.pleq = pleq;
		this.monitor = monitor;
		this.backend = backendQueueProcessorFactory;
		this.endSignal = endSignal;
		log.trace( "created" );
	}

	public void run() {
		log.debug( "Start" );
		try {
			while ( true ) {
				Object take = pleq.take();
				if ( take == null ) {
					break;
				}
				else {
					LuceneWork work = (LuceneWork) take;
					log.trace( "received lucenework {}", work );
					//TODO group work in bigger lists of size #batch and introduce the CommitLuceneWork to avoid waiting more work
					ArrayList<LuceneWork> list = new ArrayList<LuceneWork>( 1 );
					list.add( work );
					Runnable processor = backend.getProcessor( list );
					processor.run();
					monitor.documentsAdded( 1L );
				}
			}
			log.debug( "Finished" );
		}
		catch (InterruptedException e) {
			// normal quit: no need to propagate interruption.
			log.debug( "Interrupted" );
		}
		finally {
			//notify everybody we have finished.
			endSignal.countDown();
		}
	}

}