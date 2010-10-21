/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.configuration;

import java.io.IOException;
import java.util.Properties;

import org.hibernate.search.backend.LuceneIndexingParameters;
import org.hibernate.search.test.Document;
import org.hibernate.search.test.SerializationTestHelper;
import org.hibernate.search.test.query.Author;
import org.hibernate.search.test.query.Book;

import static org.hibernate.search.backend.configuration.IndexWriterSetting.MAX_BUFFERED_DOCS;
import static org.hibernate.search.backend.configuration.IndexWriterSetting.MAX_MERGE_DOCS;
import static org.hibernate.search.backend.configuration.IndexWriterSetting.MERGE_FACTOR;
import static org.hibernate.search.backend.configuration.IndexWriterSetting.RAM_BUFFER_SIZE;
import static org.hibernate.search.backend.configuration.IndexWriterSetting.USE_COMPOUND_FILE;
import static org.hibernate.search.backend.configuration.IndexWriterSetting.MAX_FIELD_LENGTH;
import static org.hibernate.search.test.configuration.ConfigurationReadTestCase.TransactionType.TRANSACTION;
import static org.hibernate.search.test.configuration.ConfigurationReadTestCase.TransactionType.BATCH;

/**
 * @author Sanne Grinovero
 */
public class LuceneIndexingParametersTest extends ConfigurationReadTestCase {
	
	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		
		cfg.setProperty( "hibernate.search.default.batch.ram_buffer_size", "1" );
		cfg.setProperty( "hibernate.search.default.transaction.use_compound_file", "false" );
		cfg.setProperty( "hibernate.search.default.batch.use_compound_file", "true" ); //should see a warning about this
//set by super : cfg.setProperty( "hibernate.search.default.batch.max_buffered_docs", "1000" );
		
		cfg.setProperty( "hibernate.search.default.transaction.ram_buffer_size", "2" );
		cfg.setProperty( "hibernate.search.default.transaction.max_merge_docs", "9" );
//set by super : cfg.setProperty( "hibernate.search.default.transaction.merge_factor", "100" );
		cfg.setProperty( "hibernate.search.default.transaction.max_buffered_docs", "11" );
		
		cfg.setProperty( "hibernate.search.Book.batch.max_merge_docs", "12" );
		cfg.setProperty( "hibernate.search.Book.transaction.use_compound_file", "false" );
		cfg.setProperty( "hibernate.search.Book.batch.merge_factor", "13" );
		// new keyword "indexwriter" is also supported to group parameters:
		cfg.setProperty( "hibernate.search.Book.indexwriter.batch.max_buffered_docs", "14" );
		
		cfg.setProperty( "hibernate.search.Book.indexwriter.transaction.ram_buffer_size", "4" );
		cfg.setProperty( "hibernate.search.Book.transaction.max_merge_docs", "15" );
		cfg.setProperty( "hibernate.search.Book.transaction.merge_factor", "16" );
		cfg.setProperty( "hibernate.search.Book.transaction.max_buffered_docs", "17" );
		
		cfg.setProperty( "hibernate.search.Documents.transaction.ram_buffer_size", "default" );
		cfg.setProperty( "hibernate.search.Documents.transaction.max_merge_docs", "5" );
		cfg.setProperty( "hibernate.search.Documents.transaction.merge_factor", "6" );
		cfg.setProperty( "hibernate.search.Documents.transaction.max_buffered_docs", "7" );
		cfg.setProperty( "hibernate.search.Documents.batch.max_merge_docs", "9" );
		cfg.setProperty( "hibernate.search.Documents.transaction.max_field_length", "7" );
		cfg.setProperty( "hibernate.search.Documents.batch.max_field_length", "9" );
	}
	
	public void testDefaultIndexProviderParameters() {
		assertValueIsSet( Author.class, BATCH, USE_COMPOUND_FILE, 1 );
		assertValueIsSet( Author.class, TRANSACTION, RAM_BUFFER_SIZE, 2 );
		assertValueIsSet( Author.class, TRANSACTION, MAX_MERGE_DOCS, 9 );
		assertValueIsSet( Author.class, TRANSACTION, MAX_BUFFERED_DOCS,  11 );
		assertValueIsSet( Author.class, TRANSACTION, MERGE_FACTOR,  100 );
	}
	
	public void testBatchParametersGlobals() {
		assertValueIsSet( Author.class, BATCH, RAM_BUFFER_SIZE, 1 );
		assertValueIsDefault( Author.class, BATCH, MAX_MERGE_DOCS );
		assertValueIsSet( Author.class, BATCH, MAX_BUFFERED_DOCS, 1000 );
	}
	
	public void testMaxFieldLength() {
		// there should also be logged a warning being logged about these:
		assertValueIsSet( Document.class, TRANSACTION, MAX_FIELD_LENGTH, 7 );
		assertValueIsSet( Document.class, BATCH, MAX_FIELD_LENGTH, 9 );
	}
	
	public void testExplicitBatchParameters() {
		assertValueIsSet( Book.class, BATCH, MAX_MERGE_DOCS, 12 );
		assertValueIsSet( Book.class, BATCH, MAX_BUFFERED_DOCS, 14 );
		assertValueIsSet( Book.class, BATCH, MERGE_FACTOR, 13 );
		assertValueIsSet( Book.class, TRANSACTION, USE_COMPOUND_FILE, 0 );
	}
	
	public void testInheritedBatchParameters() {
		assertValueIsSet( Book.class, BATCH, RAM_BUFFER_SIZE, 1 );
	}
	
	public void testTransactionParameters() {
		assertValueIsSet( Book.class, TRANSACTION, RAM_BUFFER_SIZE, 4 );
		assertValueIsSet( Book.class, TRANSACTION, MAX_MERGE_DOCS, 15 );
		assertValueIsSet( Book.class, TRANSACTION, MAX_BUFFERED_DOCS, 17 );
		assertValueIsSet( Book.class, TRANSACTION, MERGE_FACTOR, 16 );
	}
	
	public void testDefaultKeywordOverwritesInherited() {
		assertValueIsDefault( Document.class, TRANSACTION, RAM_BUFFER_SIZE );
		assertValueIsDefault( Document.class, TRANSACTION, RAM_BUFFER_SIZE );
	}
	
	public void testSerializability() throws IOException, ClassNotFoundException {
		LuceneIndexingParameters param = new LuceneIndexingParameters( new Properties() );
		LuceneIndexingParameters paramCopy = (LuceneIndexingParameters)
			SerializationTestHelper.duplicateBySerialization( param );
		assertEquals( param.getBatchIndexParameters(), paramCopy.getBatchIndexParameters() );
		assertEquals( param.getTransactionIndexParameters(), paramCopy.getTransactionIndexParameters() );
	}
	
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Book.class,
				Author.class,
				Document.class
		};
	}
	
}
