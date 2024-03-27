/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.writer.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.search.backend.lucene.logging.impl.LuceneLogCategories;
import org.hibernate.search.util.impl.test.extension.log4j.Log4j2ConfigurationAccessor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.ByteBuffersDirectory;

class LoggerInfoStreamTest {

	private static final String LOGGER_NAME = LuceneLogCategories.INFOSTREAM_LOGGER_CATEGORY.getName();

	private final Log4j2ConfigurationAccessor programmaticConfig;
	private TestAppender testAppender;

	public LoggerInfoStreamTest() {
		programmaticConfig = new Log4j2ConfigurationAccessor( LOGGER_NAME );
	}

	@BeforeEach
	void setUp() throws Exception {
		testAppender = new TestAppender( "LuceneTestAppender" );
		programmaticConfig.addAppender( testAppender );
	}

	@AfterEach
	void tearDown() throws Exception {
		programmaticConfig.removeAppender();
	}

	@Test
	void testEnableInfoStream() throws Exception {
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

		List<String> logEvents = testAppender.searchByLoggerAndMessage( LOGGER_NAME, "IW:" );

		assertThat( logEvents ).isNotEmpty();
	}
}
