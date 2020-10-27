/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.writer.impl;

import static org.junit.Assert.assertFalse;

import java.util.List;

import org.hibernate.search.backend.lucene.logging.impl.LuceneLogCategories;
import org.hibernate.search.util.impl.test.rule.log4j.Log4j2ConfigurationAccessor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.ByteBuffersDirectory;

public class LoggerInfoStreamTest {

	private final Log4j2ConfigurationAccessor programmaticConfig;
	private TestAppender testAppender;

	public LoggerInfoStreamTest() {
		programmaticConfig = new Log4j2ConfigurationAccessor();
	}

	@Before
	public void setUp() throws Exception {
		testAppender = new TestAppender( "LuceneTestAppender" );
		programmaticConfig.addAppender( testAppender );
	}

	@After
	public void tearDown() throws Exception {
		programmaticConfig.removeAppender();
	}

	@Test
	public void testEnableInfoStream() throws Exception {
		LoggerInfoStream infoStream = new LoggerInfoStream();

		ByteBuffersDirectory directory = new ByteBuffersDirectory();
		IndexWriterConfig indexWriterConfig = new IndexWriterConfig( new StandardAnalyzer() );
		indexWriterConfig.setInfoStream( infoStream );

		IndexWriter indexWriter = new IndexWriter( directory, indexWriterConfig );
		Document doc = new Document();
		doc.add( new StringField( "f1", "value1", Field.Store.YES ) );

		indexWriter.addDocument( doc );
		indexWriter.commit();
		indexWriter.close();

		List<String> logEvents = testAppender.searchByLoggerAndMessage(
				LuceneLogCategories.INFOSTREAM_LOGGER_CATEGORY.getName(), "IW:"
		);

		assertFalse( logEvents.isEmpty() );
	}
}
