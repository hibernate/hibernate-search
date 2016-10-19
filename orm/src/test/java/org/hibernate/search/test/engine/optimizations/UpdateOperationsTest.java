/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine.optimizations;

import java.util.List;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.backend.impl.lucene.AbstractWorkspaceImpl;
import org.hibernate.search.test.Document;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.hibernate.search.testsupport.backend.LeakingLocalBackend;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.hibernate.search.testsupport.optimizer.LeakingOptimizer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test that {@link AbstractWorkspaceImpl} uses
 * {@link org.apache.lucene.index.IndexWriter#updateDocument(Term, Iterable<? extends IndexableField>)}
 * for updating documents when possible.
 *
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 */
@Category( SkipOnElasticsearch.class ) // This optimization is specific to the Lucene backend
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
			LeakingLocalBackend.reset();
			FullTextSession session = fullTextSessionBuilder.openFullTextSession();
			Assert.assertEquals( 0, LeakingOptimizer.getTotalOperations() );

			Transaction tx = session.beginTransaction();
			session.persist( new Document( "The Book", "many paper pages assembled together at one side", "[old language you don't understand]" ) );
			tx.commit();

			Assert.assertEquals( 1, LeakingOptimizer.getTotalOperations() );
			Assert.assertEquals( 1, LeakingLocalBackend.getLastProcessedQueue().size() );

			tx = session.beginTransaction();
			List list = session.createFullTextQuery( new MatchAllDocsQuery() ).list();
			Document doc = (Document) list.get( 0 );
			doc.setSummary( "Example of what was used in ancient times to read" );
			tx.commit();

			Assert.assertEquals( 1, LeakingLocalBackend.getLastProcessedQueue().size() );
			Assert.assertEquals( expectedBackendOperations, LeakingOptimizer.getTotalOperations() );
		}
		finally {
			fullTextSessionBuilder.close();
		}
	}

	private static FullTextSessionBuilder createSearchFactory(boolean indexMetadataIsComplete) {
		FullTextSessionBuilder builder = new FullTextSessionBuilder()
				.setProperty( "hibernate.search.default.worker.backend", LeakingLocalBackend.class.getName() )
				.setProperty( "hibernate.search.default.optimizer.implementation", LeakingOptimizer.class.getCanonicalName() )
				.addAnnotatedClass( Document.class );
		if ( !indexMetadataIsComplete ) {
			builder.setProperty( "hibernate.search.default.index_metadata_complete", "false" );
		}
		return builder.build();
	}

}
