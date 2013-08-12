/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

package org.hibernate.search.test.util;

import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.impl.SimpleIndexingProgressMonitor;

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
