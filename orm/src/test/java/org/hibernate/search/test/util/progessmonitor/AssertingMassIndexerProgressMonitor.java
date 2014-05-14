/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.util.progessmonitor;

import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.batchindexing.impl.SimpleIndexingProgressMonitor;

import static org.junit.Assert.assertEquals;

/**
 * @author Hardy Ferentschik
 */
public class AssertingMassIndexerProgressMonitor implements MassIndexerProgressMonitor {

	private final MassIndexerProgressMonitor monitor;
	private final AtomicLong totalCount = new AtomicLong();
	private final AtomicLong finishedCount = new AtomicLong();
	private final AtomicLong addedDocuments = new AtomicLong();
	private final int expectedAddedDocuments;
	private final int expectedTotalCount;

	public AssertingMassIndexerProgressMonitor(int expectedAddedDocuments, int expectedTotalCount) {
		this.expectedAddedDocuments = expectedAddedDocuments;
		this.expectedTotalCount = expectedTotalCount;
		this.monitor = new SimpleIndexingProgressMonitor( 1 );
	}

	@Override
	public void documentsAdded(long increment) {
		addedDocuments.addAndGet( increment );
		monitor.documentsAdded( increment );
	}

	@Override
	public void documentsBuilt(int number) {
		monitor.documentsBuilt( number );
	}

	@Override
	public void entitiesLoaded(int size) {
		monitor.entitiesLoaded( size );
	}

	@Override
	public void addToTotalCount(long count) {
		totalCount.addAndGet( count );
		monitor.addToTotalCount( count );
	}

	@Override
	public void indexingCompleted() {
		finishedCount.incrementAndGet();
	}

	public void assertExpectedProgressMade() {
		assertEquals( "Unexpected number of added documents", expectedAddedDocuments, addedDocuments.get() );
		assertEquals( "Unexpected total count", expectedTotalCount, totalCount.get() );
		assertEquals( "Finished called more than once", 1, finishedCount.get() );
	}
}
