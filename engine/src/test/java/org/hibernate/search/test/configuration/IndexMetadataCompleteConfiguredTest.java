/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import java.lang.annotation.ElementType;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.backend.impl.lucene.AbstractWorkspaceImpl;
import org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessor;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.impl.MutableSearchFactory;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * Verifies the global setting from {@link org.hibernate.search.cfg.spi.SearchConfiguration#isIndexMetadataComplete()}
 * affect the backends as expected.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class IndexMetadataCompleteConfiguredTest {

	@Test
	public void testDefaultImplementation() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		verifyIndexCompleteMetadataOption( true, cfg );
	}

	@Test
	public void testIndexMetadataCompleteFalse() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.setIndexMetadataComplete( false );
		verifyIndexCompleteMetadataOption( false, cfg );
	}

	@Test
	public void testIndexMetadataCompleteTrue() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.setIndexMetadataComplete( true );
		verifyIndexCompleteMetadataOption( true, cfg );
	}

	private void verifyIndexCompleteMetadataOption(boolean expectation, SearchConfigurationForTest cfg) {
		SearchMapping mapping = new SearchMapping();
		mapping
			.entity( Document.class ).indexed().indexName( "index1" )
			.property( "id", ElementType.FIELD ).documentId()
			.property( "title", ElementType.FIELD ).field()
			;
		cfg.setProgrammaticMapping( mapping );
		cfg.addClass( Document.class );
		MutableSearchFactory sf = (MutableSearchFactory) new SearchIntegratorBuilder().configuration( cfg ).buildSearchIntegrator();
		try {
			assertEquals( expectation, extractWorkspace( sf, Document.class ).areSingleTermDeletesSafe() );

			// trigger a SearchFactory rebuild:
			sf.addClasses( Dvd.class, Book.class );
			// DVD share the same index, so now it's always unsafe [always false no matter the global option]
			assertEquals( false, extractWorkspace( sf, Dvd.class ).areSingleTermDeletesSafe() );
			assertEquals( false, extractWorkspace( sf, Document.class ).areSingleTermDeletesSafe() );

			// but still as expected for Book :
			assertEquals( expectation, extractWorkspace( sf, Book.class ).areSingleTermDeletesSafe() );
		}
		finally {
			sf.close();
		}
	}

	private static AbstractWorkspaceImpl extractWorkspace(MutableSearchFactory sf, Class<?> type) {
		EntityIndexBinding indexBindingForEntity = sf.getIndexBinding( type );
		DirectoryBasedIndexManager indexManager = (DirectoryBasedIndexManager) indexBindingForEntity.getIndexManagers()[0];
		LuceneBackendQueueProcessor backend = (LuceneBackendQueueProcessor) indexManager.getBackendQueueProcessor();
		return backend.getIndexResources().getWorkspace();
	}

	public static final class Document {
		long id;
		String title;
	}

	@Indexed(index = "index1")
	public static final class Dvd {
		@DocumentId long id;
		@Field String title;
	}

	@Indexed(index = "index2")
	public static final class Book {
		@DocumentId long id;
		@Field String title;
	}
}
