/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.util.logging;

import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.RAMDirectory;
import org.hibernate.search.util.logging.impl.LoggerInfoStream;
import org.hibernate.search.util.logging.impl.LuceneLogCategories;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertFalse;

public class LoggerInfoStreamTest {

	private Level hsearchLevel;
	private final Logger hsearchLogger = Logger.getLogger( "org.hibernate.search" );
	private final Logger rootLogger = Logger.getRootLogger();
	private TestAppender testAppender;

	@Before
	public void setUp() throws Exception {
		testAppender = new TestAppender();
		rootLogger.addAppender( testAppender );
		hsearchLevel = hsearchLogger.getLevel();
		hsearchLogger.setLevel( Level.TRACE );
	}

	@After
	public void tearDown() throws Exception {
		rootLogger.removeAppender( testAppender );
		hsearchLogger.setLevel( hsearchLevel );
	}

	@Test
	public void testEnableInfoStream() throws Exception {
		LoggerInfoStream infoStream = new LoggerInfoStream();

		RAMDirectory directory = new RAMDirectory();
		IndexWriterConfig indexWriterConfig = new IndexWriterConfig( new StandardAnalyzer() );
		indexWriterConfig.setInfoStream( infoStream );

		IndexWriter indexWriter = new IndexWriter( directory, indexWriterConfig );
		Document doc = new Document();
		doc.add( new StringField( "f1", "value1", Field.Store.YES ) );

		indexWriter.addDocument( doc );
		indexWriter.commit();
		indexWriter.close();

		List<LoggingEvent> loggingEvents = testAppender.searchByLoggerAndMessage( LuceneLogCategories.INFOSTREAM_LOGGER_CATEGORY.getName(), "IW:" );

		assertFalse( loggingEvents.isEmpty() );
	}
}
