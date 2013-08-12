/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.hibernate.search.test.batchindexing;

import org.apache.lucene.search.MatchAllDocsQuery;

import org.hibernate.Transaction;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.util.AssertingMassIndexerProgressMonitor;

/**
 * @author Hardy Ferentschik
 */
public class ProgressMonitorTest extends SearchTestCase {
	FullTextSession fullTextSession;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		fullTextSession = Search.getFullTextSession( openSession() );
		initializeData( fullTextSession );
	}

	public void testAllRelevantProgressMonitoringOperationsCalled() throws InterruptedException {
		// let mass indexer re-index the data in the db (created in initializeData())
		AssertingMassIndexerProgressMonitor monitor = new AssertingMassIndexerProgressMonitor( 10, 10 );
		fullTextSession.createIndexer( LegacyCar.class )
				.progressMonitor( monitor )
				.startAndWait();
		fullTextSession.createFullTextQuery( new MatchAllDocsQuery(), LegacyCar.class )
				.getResultSize();
		monitor.assertExpectedProgressMade();
	}

	private static void initializeData(FullTextSession fullTextSession) {
		final Transaction transaction = fullTextSession.beginTransaction();
		LegacyCar[] cars = new LegacyCar[10];
		for ( int i = 0; i < cars.length; i++ ) {
			cars[i] = new LegacyCar();
			cars[i].setId( "" + i );
			cars[i].setModel( "model" + i );
			fullTextSession.persist( cars[i] );
		}
		transaction.commit();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { LegacyCarPlant.class, LegacyCar.class, LegacyTire.class };
	}
}
