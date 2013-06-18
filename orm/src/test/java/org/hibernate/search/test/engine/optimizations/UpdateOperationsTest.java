/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.search.test.engine.optimizations;


import java.util.List;

import junit.framework.Assert;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.test.Document;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.hibernate.search.test.util.LeakingLuceneBackend;
import org.hibernate.search.test.util.LeakingOptimizer;
import org.junit.Test;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class UpdateOperationsTest {

	@Test
	public void testBackendOperationsCount() {
		invokeTest( true, 2 );
	}

	@Test
	public void testDisablingOptimization() {
		invokeTest( false, 3 );
	}

	private void invokeTest(boolean indexMetadataIsComplete, int expectedBackendOperations) {
		FullTextSessionBuilder fullTextSessionBuilder = createSearchFactory( indexMetadataIsComplete );
		try {
			LeakingOptimizer.reset();
			LeakingLuceneBackend.reset();
			FullTextSession session = fullTextSessionBuilder.openFullTextSession();
			Assert.assertEquals( 0, LeakingOptimizer.getTotalOperations() );

			Transaction tx = session.beginTransaction();
			session.persist( new Document( "The Book", "many paper pages assembled together at one side", "[old language you don't understand]" ) );
			tx.commit();

			Assert.assertEquals( 1, LeakingOptimizer.getTotalOperations() );
			Assert.assertEquals( 1, LeakingLuceneBackend.getLastProcessedQueue().size() );

			tx = session.beginTransaction();
			List list = session.createFullTextQuery( new MatchAllDocsQuery() ).list();
			Document doc = (Document) list.get( 0 );
			doc.setSummary( "Example of what was used in ancient times to read" );
			tx.commit();

			Assert.assertEquals( 1, LeakingLuceneBackend.getLastProcessedQueue().size() );
			Assert.assertEquals( expectedBackendOperations, LeakingOptimizer.getTotalOperations() );
		}
		finally {
			fullTextSessionBuilder.close();
		}
	}

	private static FullTextSessionBuilder createSearchFactory(boolean indexMetadataIsComplete) {
		FullTextSessionBuilder builder = new FullTextSessionBuilder()
				.setProperty( "hibernate.search.default.worker.backend", LeakingLuceneBackend.class.getName() )
				.setProperty( "hibernate.search.default.optimizer.implementation", LeakingOptimizer.class.getCanonicalName() )
				.addAnnotatedClass( Document.class );
		if ( !indexMetadataIsComplete ) {
			builder.setProperty( "hibernate.search.default.index_metadata_complete", "false" );
		}
		return builder.build();
	}

}
