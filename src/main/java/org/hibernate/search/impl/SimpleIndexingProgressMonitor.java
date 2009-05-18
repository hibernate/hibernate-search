package org.hibernate.search.impl;

import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.search.batchindexing.IndexerProgressMonitor;
import org.hibernate.search.util.LoggerFactory;
import org.slf4j.Logger;

/**
 * A very simple implementation of IndexerProgressMonitor
 * 
 * @author Sanne Grinovero
 */
public class SimpleIndexingProgressMonitor implements IndexerProgressMonitor {
	
	private static final Logger log = LoggerFactory.make();
	private final AtomicLong documentsDoneCounter = new AtomicLong();
	private final AtomicLong totalCounter = new AtomicLong();
	private volatile long startTimeMs;

	public void entitiesLoaded(int size) {
		//not used
	}

	public void documentsAdded(long increment) {
		long current = documentsDoneCounter.addAndGet( increment );
		if ( current == increment ) {
			startTimeMs = System.currentTimeMillis();
		}
		if ( current % getStatusMessagePeriod() == 0 ) {
			printStatusMessage( startTimeMs, totalCounter.get(), current );
		}
	}

	public void documentsBuilt(int number) {
		//not used
	}

	public void addToTotalCount(long count) {
		totalCounter.addAndGet( count );
		log.info( "Going to reindex {} entities", count );
	}
	
	protected int getStatusMessagePeriod() {
		return 1000;
	}
	
	protected void printStatusMessage(long starttimems, long totalTodoCount, long doneCount) {
		long elapsedMs = System.currentTimeMillis() - starttimems;
		log.info( "{} documents indexed in {} ms", doneCount, elapsedMs );
		double estimateSpeed = ( (double) doneCount * 1000f ) / elapsedMs ;
		double estimatePercentileComplete = ( (double) doneCount*100 ) / (double)totalTodoCount ;
		log.info( "Indexing speed: {} documents/second; progress: {}%", estimateSpeed, estimatePercentileComplete );
	}

}
