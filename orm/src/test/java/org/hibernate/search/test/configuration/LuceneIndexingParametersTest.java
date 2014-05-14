/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import java.io.IOException;
import java.util.Properties;

import org.hibernate.search.backend.spi.LuceneIndexingParameters;
import org.hibernate.search.test.Document;
import org.hibernate.search.test.SerializationTestHelper;
import org.hibernate.search.test.query.Author;
import org.hibernate.search.test.query.Book;
import org.junit.Test;

import static org.hibernate.search.backend.configuration.impl.IndexWriterSetting.MAX_BUFFERED_DOCS;
import static org.hibernate.search.backend.configuration.impl.IndexWriterSetting.MAX_MERGE_DOCS;
import static org.hibernate.search.backend.configuration.impl.IndexWriterSetting.MERGE_CALIBRATE_BY_DELETES;
import static org.hibernate.search.backend.configuration.impl.IndexWriterSetting.MERGE_FACTOR;
import static org.hibernate.search.backend.configuration.impl.IndexWriterSetting.MERGE_MAX_OPTIMIZE_SIZE;
import static org.hibernate.search.backend.configuration.impl.IndexWriterSetting.RAM_BUFFER_SIZE;
import static org.junit.Assert.assertEquals;

/**
 * @author Sanne Grinovero
 */
public class LuceneIndexingParametersTest extends ConfigurationReadTestCase {

	@Override
	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );

		cfg.setProperty( "hibernate.search.default.indexwriter.ram_buffer_size", "1" );
		cfg.setProperty( "hibernate.search.default.indexwriter.merge_calibrate_by_deletes", "false" );
//set by super : cfg.setProperty( "hibernate.search.default.indexwriter.max_buffered_docs", "1000" );
		cfg.setProperty( "hibernate.search.default.indexwriter.max_merge_docs", "9" );
//set by super : cfg.setProperty( "hibernate.search.default.indexwriter.merge_factor", "100" );

		cfg.setProperty( "hibernate.search.Book.indexwriter.max_merge_docs", "12" );
		cfg.setProperty( "hibernate.search.Book.indexwriter.merge_calibrate_by_deletes", "false" );
		cfg.setProperty( "hibernate.search.Book.indexwriter.merge_factor", "13" );
		cfg.setProperty( "hibernate.search.Book.indexwriter.max_buffered_docs", "14" );
		cfg.setProperty( "hibernate.search.Book.indexwriter.ram_buffer_size", "4" );
		cfg.setProperty( "hibernate.search.Book.indexwriter.merge_max_optimize_size", "256");

		cfg.setProperty( "hibernate.search.Documents.indexwriter.ram_buffer_size", "default" );
		cfg.setProperty( "hibernate.search.Documents.indexwriter.merge_factor", "6" );
		cfg.setProperty( "hibernate.search.Documents.indexwriter.max_buffered_docs", "7" );
		cfg.setProperty( "hibernate.search.Documents.indexwriter.max_merge_docs", "9" );
		cfg.setProperty( "hibernate.search.Documents.indexwriter.max_field_length", "9" );
	}

	@Test
	public void testDefaultIndexProviderParameters() {
		assertValueIsSet( Author.class, MERGE_CALIBRATE_BY_DELETES, 0 );
		assertValueIsSet( Author.class, RAM_BUFFER_SIZE, 1 );
		assertValueIsSet( Author.class, MAX_MERGE_DOCS, 9 );
		assertValueIsSet( Author.class, MAX_BUFFERED_DOCS, 1000 );
		assertValueIsSet( Author.class, MERGE_FACTOR, 100 );
	}

	@Test
	public void testSpecificTypeParametersOverride() {
		assertValueIsSet( Book.class, MAX_MERGE_DOCS, 12 );
		assertValueIsSet( Book.class, MAX_BUFFERED_DOCS, 14 );
		assertValueIsSet( Book.class, MERGE_FACTOR, 13 );
		assertValueIsSet( Book.class, MERGE_CALIBRATE_BY_DELETES, 0 );
		assertValueIsSet( Book.class, RAM_BUFFER_SIZE, 4 );
		assertValueIsSet( Book.class, MERGE_MAX_OPTIMIZE_SIZE, 256 );
	}

	@Test
	public void testDefaultKeywordOverwritesInherited() {
		assertValueIsDefault( Document.class, RAM_BUFFER_SIZE );
	}

	@Test
	public void testSerializability() throws IOException, ClassNotFoundException {
		LuceneIndexingParameters param = new LuceneIndexingParameters( new Properties() );
		LuceneIndexingParameters paramCopy = (LuceneIndexingParameters)
			SerializationTestHelper.duplicateBySerialization( param );
		assertEquals( param.getIndexParameters(), paramCopy.getIndexParameters() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Book.class,
				Author.class,
				Document.class
		};
	}

}
