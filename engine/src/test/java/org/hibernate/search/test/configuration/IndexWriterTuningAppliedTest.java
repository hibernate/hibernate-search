/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.backend.impl.lucene.AbstractWorkspaceImpl;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.hibernate.search.util.logging.impl.LuceneLogCategories;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * We already have plenty of tests verifying the parsing of configuration properties,
 * so this test actually verifies that the property is also being applied on the
 * IndexWriter.
 *
 * @author Sanne Grinovero
 */
@TestForIssue(jiraKey = "HSEARCH-1508")
@Category(SkipOnElasticsearch.class) // IndexWriters are specific to Lucene
public class IndexWriterTuningAppliedTest {

	@Rule
	public SearchFactoryHolder sfHolder = new SearchFactoryHolder( Dvd.class, Book.class )
		.withProperty( "hibernate.search.default.indexwriter.max_thread_states", "23" )
		.withProperty( "hibernate.search.index2.indexwriter.max_thread_states", "7" )
		.withProperty( "hibernate.search.index2.indexwriter.infostream", "true" );

	private final IndexedTypeIdentifier dvdTestType = PojoIndexedTypeIdentifier.convertFromLegacy( Dvd.class );
	private final IndexedTypeIdentifier bookTestType = PojoIndexedTypeIdentifier.convertFromLegacy( Book.class );

	@Test
	public void testInfoStream() throws IOException {
		//Enable trace level on the magic category:
		Logger.getLogger( LuceneLogCategories.INFOSTREAM_LOGGER_CATEGORY.getName() ).setLevel( Level.TRACE );
		AbstractWorkspaceImpl dvdsWorkspace = sfHolder.extractWorkspace( dvdTestType );
		AbstractWorkspaceImpl booksWorkspace = sfHolder.extractWorkspace( bookTestType );
		IndexWriter dvdsIndexWriter = dvdsWorkspace.getIndexWriter();
		IndexWriter booksIndexWriter = booksWorkspace.getIndexWriter();
		try {
			Assert.assertFalse( dvdsIndexWriter.getConfig().getInfoStream().isEnabled( "IW" ) );
			Assert.assertTrue( booksIndexWriter.getConfig().getInfoStream().isEnabled( "IW" ) );
		}
		finally {
			booksIndexWriter.close();
			dvdsIndexWriter.close();
		}
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
