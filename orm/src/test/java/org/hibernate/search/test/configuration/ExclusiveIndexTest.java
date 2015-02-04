/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import static org.junit.Assert.assertEquals;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.backend.impl.lucene.AbstractWorkspaceImpl;
import org.hibernate.search.backend.impl.lucene.ExclusiveIndexWorkspaceImpl;
import org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessor;
import org.hibernate.search.backend.impl.lucene.SharedIndexWorkspaceImpl;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.junit.Test;

/**
 * Verifies the property exclusive_index_use is properly applied to the backend
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class ExclusiveIndexTest {

	@Test
	public void verifyIndexExclusivity() {
		FullTextSessionBuilder builder = new FullTextSessionBuilder();
		FullTextSession ftSession = builder
			.setProperty(
					"hibernate.search.org.hibernate.search.test.configuration.BlogEntry.exclusive_index_use",
					"true"
			)
			.setProperty( "hibernate.search.Book.exclusive_index_use", "false" )
			.addAnnotatedClass( BlogEntry.class )
			.addAnnotatedClass( Foo.class )
			.addAnnotatedClass( org.hibernate.search.test.query.Book.class )
			.addAnnotatedClass( org.hibernate.search.test.query.Author.class )
			.openFullTextSession();
		ExtendedSearchIntegrator integrator = ftSession.getSearchFactory().unwrap( ExtendedSearchIntegrator.class );
		ftSession.close();
		IndexManagerHolder allIndexesManager = integrator.getIndexManagerHolder();
		//explicitly enabled:
		assertExclusiveIsEnabled( allIndexesManager, "org.hibernate.search.test.configuration.BlogEntry", true );
		//explicitly disabled (this entity defined a short index name):
		assertExclusiveIsEnabled( allIndexesManager, "Book", false );
		//using default:
		assertExclusiveIsEnabled( allIndexesManager, Foo.class.getName(), true );
		builder.close();
	}

	private void assertExclusiveIsEnabled(IndexManagerHolder allIndexesManager, String indexName, boolean expectExclusive) {
		DirectoryBasedIndexManager indexManager = (DirectoryBasedIndexManager) allIndexesManager.getIndexManager( indexName );
		BackendQueueProcessor backendQueueProcessor = indexManager.getBackendQueueProcessor();
		assertEquals( LuceneBackendQueueProcessor.class, backendQueueProcessor.getClass() );
		LuceneBackendQueueProcessor backend = (LuceneBackendQueueProcessor) backendQueueProcessor;
		AbstractWorkspaceImpl workspace = backend.getIndexResources().getWorkspace();
		if ( expectExclusive ) {
			assertEquals( ExclusiveIndexWorkspaceImpl.class, workspace.getClass() );
		}
		else {
			assertEquals( SharedIndexWorkspaceImpl.class, workspace.getClass() );
		}
	}

	@Indexed
	@Entity
	@Table(name = "Foo")
	public static class Foo {

		@Id
		private int id;
	}
}
