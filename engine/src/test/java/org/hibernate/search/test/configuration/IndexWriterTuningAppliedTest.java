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
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.util.logging.impl.LoggerInfoStream;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * We already have plenty of tests verifying the parsing of configuration properties,
 * so this test actually verifies that the property is also being applied on the
 * IndexWriter.
 *
 * @author Sanne Grinovero
 */
@TestForIssue(jiraKey = "HSEARCH-1508")
public class IndexWriterTuningAppliedTest {

	@Rule
	public SearchFactoryHolder sfHolder = new SearchFactoryHolder( Dvd.class, Book.class )
		.withProperty( "hibernate.search.default.indexwriter.max_thread_states", "23" )
		.withProperty( "hibernate.search.index2.indexwriter.max_thread_states", "7" )
		.withProperty( "hibernate.search.index2.indexwriter.infostream", "true" );

	@Test
	public void testIndexWriterTuningApplied() throws IOException {
		AbstractWorkspaceImpl dvdsWorkspace = sfHolder.extractWorkspace( Dvd.class );
		IndexWriter dvdsIndexWriter = dvdsWorkspace.getIndexWriter();
		try {
			Assert.assertEquals( 23, dvdsIndexWriter.getConfig().getMaxThreadStates() );
		}
		finally {
			dvdsIndexWriter.close( false );
		}
	}

	@Test
	public void testInfoStream() throws IOException {
		//Enable trace level on the magic category:
		Logger.getLogger( LoggerInfoStream.INFOSTREAM_LOGGER_CATEGORY ).setLevel( Level.TRACE );
		AbstractWorkspaceImpl dvdsWorkspace = sfHolder.extractWorkspace( Dvd.class );
		AbstractWorkspaceImpl booksWorkspace = sfHolder.extractWorkspace( Book.class );
		IndexWriter dvdsIndexWriter = dvdsWorkspace.getIndexWriter();
		IndexWriter booksIndexWriter = booksWorkspace.getIndexWriter();
		try {
			Assert.assertFalse( dvdsIndexWriter.getConfig().getInfoStream().isEnabled( "IW" ) );
			Assert.assertTrue( booksIndexWriter.getConfig().getInfoStream().isEnabled( "IW" ) );
		}
		finally {
			booksIndexWriter.close( false );
			dvdsIndexWriter.close( false );
		}
	}

	@Test
	public void testIndexWriterTuningAppliedOnDefault() throws IOException {
		AbstractWorkspaceImpl booksWorkspace = sfHolder.extractWorkspace( Book.class );
		IndexWriter booksIndexWriter = booksWorkspace.getIndexWriter();
		try {
			Assert.assertEquals( 7, booksIndexWriter.getConfig().getMaxThreadStates() );
		}
		finally {
			booksIndexWriter.close( false );
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
