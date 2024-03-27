/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.test.util.progessmonitor;

import static org.assertj.core.api.Assertions.assertThat;

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
		assertThat( addedDocuments.longValue() ).as( "Unexpected number of added documents" )
				.isEqualTo( expectedAddedDocuments );
		assertThat( totalCount.longValue() ).as( "Unexpected total count" ).isEqualTo( expectedTotalCount );
		assertThat( finishedCount.longValue() ).as( "Finished called more than once" ).isEqualTo( 1 );
	}

}
