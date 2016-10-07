/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.hibernate.search.backend.spi.LuceneIndexingParameters;
import org.hibernate.search.test.Document;
import org.hibernate.search.test.query.Author;
import org.hibernate.search.test.query.Book;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.hibernate.search.testsupport.serialization.SerializationTestHelper;
import org.junit.Test;
import org.junit.experimental.categories.Category;

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
@Category(SkipOnElasticsearch.class) // These parameters are specific to the Lucene backend
public class LuceneIndexingParametersTest extends ConfigurationReadTestCase {

	@Override
	public void configure(Map<String,Object> cfg) {
		super.configure( cfg );

		cfg.put( "hibernate.search.default.indexwriter.ram_buffer_size", "1" );
		cfg.put( "hibernate.search.default.indexwriter.merge_calibrate_by_deletes", "false" );
//set by super : cfg.setProperty( "hibernate.search.default.indexwriter.max_buffered_docs", "1000" );
		cfg.put( "hibernate.search.default.indexwriter.max_merge_docs", "9" );
//set by super : cfg.setProperty( "hibernate.search.default.indexwriter.merge_factor", "100" );

		cfg.put( "hibernate.search.Book.indexwriter.max_merge_docs", "12" );
		cfg.put( "hibernate.search.Book.indexwriter.merge_calibrate_by_deletes", "false" );
		cfg.put( "hibernate.search.Book.indexwriter.merge_factor", "13" );
		cfg.put( "hibernate.search.Book.indexwriter.max_buffered_docs", "14" );
		cfg.put( "hibernate.search.Book.indexwriter.ram_buffer_size", "4" );
		cfg.put( "hibernate.search.Book.indexwriter.merge_max_optimize_size", "256" );

		cfg.put( "hibernate.search.Documents.indexwriter.ram_buffer_size", "default" );
		cfg.put( "hibernate.search.Documents.indexwriter.merge_factor", "6" );
		cfg.put( "hibernate.search.Documents.indexwriter.max_buffered_docs", "7" );
		cfg.put( "hibernate.search.Documents.indexwriter.max_merge_docs", "9" );
		cfg.put( "hibernate.search.Documents.indexwriter.max_field_length", "9" );
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
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Book.class,
				Author.class,
				Document.class
		};
	}

}
