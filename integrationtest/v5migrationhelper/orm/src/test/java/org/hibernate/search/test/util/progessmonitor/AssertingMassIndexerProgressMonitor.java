/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.util.progessmonitor;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.LongAdder;

import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.test.batchindexing.TracingProgressMonitor;

/**
 * @author Hardy Ferentschik
 */
public class AssertingMassIndexerProgressMonitor implements MassIndexerProgressMonitor {

	private final MassIndexerProgressMonitor monitor;
	private final LongAdder totalCount = new LongAdder();
	private final LongAdder finishedCount = new LongAdder();
	private final LongAdder addedDocuments = new LongAdder();
	private final long expectedAddedDocuments;
	private final long expectedTotalCount;

	public AssertingMassIndexerProgressMonitor(int expectedAddedDocuments, int expectedTotalCount) {
		this.expectedAddedDocuments = expectedAddedDocuments;
		this.expectedTotalCount = expectedTotalCount;
		this.monitor = new TracingProgressMonitor();
	}

	@Override
	public void documentsAdded(long increment) {
		addedDocuments.add( increment );
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
		totalCount.add( count );
		monitor.addToTotalCount( count );
	}

	@Override
	public void indexingCompleted() {
		finishedCount.add( 1 );
		monitor.indexingCompleted();
	}

	public void assertExpectedProgressMade() {
		assertEquals( "Unexpected number of added documents", expectedAddedDocuments, addedDocuments.longValue() );
		assertEquals( "Unexpected total count", expectedTotalCount, totalCount.longValue() );
		assertEquals( "Finished called more than once", 1, finishedCount.longValue() );
	}

}
